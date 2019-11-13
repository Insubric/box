package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.model.EntityActionsRegistry
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.logic.{PgColumn, PgInformationSchema}
import ch.wsl.box.rest.utils.{Auth, BoxConf, UserProfile}
import ch.wsl.box.shared.utils.JSONUtils
import com.typesafe.config.Config
import scribe.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration._
import ch.wsl.box.rest.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, ExecutionContext, Future}

object EntityMetadataFactory extends Logging {

  val dbConf: Config = com.typesafe.config.ConfigFactory.load().as[com.typesafe.config.Config]("db")
//  private val tables:Seq[String] = dbConf.as[Seq[String]]("generator.tables")
//  private val views:Seq[String] = dbConf.as[Seq[String]]("generator.views")
//  private val excludes:Seq[String] = dbConf.as[Seq[String]]("generator.excludes")
  private val excludeFields:Seq[String] = dbConf.as[Seq[String]]("generator.excludeFields")

  private var cacheTable = Map[(String, String, String, Int), Future[JSONMetadata]]()
  private var cacheKeys = Map[String, Future[Seq[String]]]()

  def resetCache() = {
    cacheTable = Map()
    cacheKeys = Map()
  }

  def lookupField(referencingTable:String,lang:String, firstNoPK:Option[String]):String = {

    val restLookupConf = BoxConf.restLookupLabels

    val default = restLookupConf.as[Option[String]]("default").getOrElse("name")

    val myDefaultTableLookupField: String = default match {
      case "firstNoPKField" => firstNoPK.getOrElse("name")
      case JSONUtils.LANG => lang
      case _ => default
    }

    restLookupConf.as[Option[String]](referencingTable).getOrElse(myDefaultTableLookupField)
  }

  def of(table:String,lang:String, lookupMaxRows:Int = 100)(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext):Future[JSONMetadata] = {

    implicit val db = up.db

    logger.warn("searching cache table for " + Seq(up.name, table, lang, lookupMaxRows).mkString)

    cacheTable.lift((up.name, table, lang,lookupMaxRows)) match {
      case Some(r) => r
      case None => {
        logger.info(s"Metadata table cache miss! cache key: ($table,$lang,$lookupMaxRows), cache: ${cacheTable}")

        val schema = new PgInformationSchema(table, db, excludeFields)

        //    println(schema.fk)

        var constraints = List[String]()

        def field2form(field: PgColumn): Future[JSONField] = {
          for {
            fk <- schema.findFk(field.column_name)
            firstNoPK <- fk match {
              case Some(f) => firstNoPKField(f.referencingTable)
              case None => Future.successful(None)
            }
          } yield {
            fk match {
              case Some(fk) => {
                if (Await.result(EntityActionsRegistry().tableActions(fk.referencingTable).count().map(_.count), 1 second) <= lookupMaxRows) {
                  if (constraints.contains(fk.constraintName)) {
                    logger.info("error: " + fk.constraintName)
                    logger.info(field.column_name)
                    Future.successful(JSONField(field.jsonType, name = field.boxName, nullable = field.nullable))
                  } else {
                    constraints = fk.constraintName :: constraints //add fk constraint to contraint list


                    val text = lookupField(fk.referencingTable, lang, firstNoPK)

                    val model = fk.referencingTable
                    val value = fk.referencingKeys.head //todo verify for multiple keys


                    import ch.wsl.box.shared.utils.JSONUtils._
                    for {
                      keys <- keysOf(model)
                      lookupData <- EntityActionsRegistry().tableActions(model).find()
                    } yield {
                      val options = lookupData.map { lookupRow =>
                        JSONLookup(lookupRow.get(value), lookupRow.get(text))
                      }

                      JSONField(
                        field.jsonType,
                        name = field.boxName,
                        nullable = field.nullable,
                        placeholder = Some(fk.referencingTable + " Lookup"),
                        //widget = Some(WidgetsNames.select),
                        lookup = Some(JSONFieldLookup(model, JSONFieldMap(value, text), options))
                      )
                    }

                  }
                } else { //no lookup from fk
                  Future.successful(JSONField(
                    field.jsonType,
                    name = field.boxName,
                    nullable = field.nullable,
                    widget = EntityMetadataFactory.defaultWidgetMapping(field.data_type)
                  ))
                }
              }
              case _ => Future.successful(JSONField(
                field.jsonType,
                name = field.boxName,
                nullable = field.nullable,
                widget = EntityMetadataFactory.defaultWidgetMapping(field.data_type)
              ))
            }
          }

        }.flatten


        val result = for {
          c <- schema.columns
          fields <- Future.sequence(c.map(field2form))
          keys <- EntityMetadataFactory.keysOf(table)
        } yield {
          val fieldList = fields.map(_.name)
          JSONMetadata(1, table, table, fields, Layout.fromFields(fields), table, lang, fieldList, keys, None, fieldList)//, table)
        }
        if(BoxConf.enableCache) {
          logger.warn("adding to cache table " + Seq(up.name, table, lang, lookupMaxRows).mkString)
          cacheTable = cacheTable ++ Map((up.name, table, lang, lookupMaxRows) -> result)
        }
        result
      }
    }
  }
  def keysOf(table:String)(implicit ec:ExecutionContext):Future[Seq[String]] = {
    logger.info("Getting " + table + " keys")
    cacheKeys.lift((table)) match {
      case Some(r) => r
      case None => {
        logger.info(s"Metadata keys cache miss! cache key: ($table), cache: ${cacheKeys}")

        val result = new PgInformationSchema(table, Auth.adminDB).pk.map { pk => //map to enter the future
          logger.info(pk.toString)
          pk.boxKeys
        }

        if(BoxConf.enableCache) cacheKeys = cacheKeys ++ Map((table) -> result)
        result
      }
    }
  }

  def firstNoPKField(table:String)(implicit db:Database, mat:Materializer, ec:ExecutionContext):Future[Option[String]] = {
    logger.info("Getting first field of " + table + " that is not PK")
    val schema = new PgInformationSchema(table, Auth.adminDB, excludeFields)
    for {
      pks <- schema.pk.map(_.boxKeys) //todo: or boxKeys?
      c <- schema.columns
    } yield
    {
//      logger.info("PK's " + pks.mkString("-"))
//      logger.info("Columns " + c.map(_.column_name).mkString("-"))
//      logger.info("Columns " + c.map(_.column_name).diff(pks).mkString("-") + " that are not PK")
      c.map(_.column_name).diff(pks).headOption
    }
  }



  def isView(table:String)(implicit ec:ExecutionContext):Future[Boolean] =
    new PgInformationSchema(table,Auth.adminDB).pgTable.map(_.isView)  //map to enter the future


  val typesMapping =  Map(
    "integer" -> JSONFieldTypes.NUMBER,
    "character varying" -> JSONFieldTypes.STRING,
    "character" -> JSONFieldTypes.STRING,
    "smallint" -> JSONFieldTypes.NUMBER,
    "bigint" -> JSONFieldTypes.NUMBER,
    "double precision" -> JSONFieldTypes.NUMBER,
    "timestamp without time zone" -> JSONFieldTypes.DATETIME,
    "date" -> JSONFieldTypes.DATE,
    "real" -> JSONFieldTypes.NUMBER,
    "boolean" -> "boolean",
    "bytea" -> JSONFieldTypes.FILE,
    "numeric" -> JSONFieldTypes.NUMBER,
    "text" -> JSONFieldTypes.STRING,
    "USER-DEFINED" -> JSONFieldTypes.STRING,
    "time without time zone" -> JSONFieldTypes.TIME,
    "ARRAY" -> JSONFieldTypes.STRING                              //todo: works only for visualisation
  )

  val defaultWidgetMapping = Map(
    "integer" -> None,
    "character varying" -> Some(WidgetsNames.textinput),
    "character" -> Some(WidgetsNames.textinput),
    "smallint" -> None,
    "bigint" -> None,
    "double precision" -> None,
    "timestamp without time zone" -> Some(WidgetsNames.datetimePicker),
    "date" -> Some(WidgetsNames.datepicker),
    "real" -> None,
    "boolean" -> None,
    "bytea" -> None,
    "numeric" -> None,
    "text" -> Some(WidgetsNames.textinput),
    "USER-DEFINED" -> None,
    "time without time zone" -> Some(WidgetsNames.timepicker),
    "ARRAY" -> Some(WidgetsNames.textinput)                          //todo: works only for visualisation -> provide widget
  )
}