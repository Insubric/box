package ch.wsl.box.rest.metadata

import java.util.UUID

import akka.stream.Materializer
import ch.wsl.box.information_schema.{PgColumn, PgInformationSchema}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.utils.{BoxConfig, UserProfile}
import ch.wsl.box.shared.utils.JSONUtils
import com.typesafe.config.Config
import scribe.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{FullDatabase, Managed, TypeMapping}
import ch.wsl.box.model.BoxFieldAccessRegistry
import ch.wsl.box.rest.runtime.{ColType, Registry}
import ch.wsl.box.services.Services
import com.avsystem.commons.Try

import scala.concurrent.{Await, ExecutionContext, Future}


object EntityMetadataFactory extends Logging {


  val excludeFields:Seq[String] = Try{
    com.typesafe.config.ConfigFactory.load().as[Seq[String]]("db.generator.excludeFields")
  }.getOrElse(Seq())
  private val cacheTable = scala.collection.mutable.Map[(String, String, String, Int), JSONMetadata]()   //  (up.name, table, lang,lookupMaxRows)
  private val cacheKeys = scala.collection.mutable.Map[String, Seq[String]]()                            //  (table)

  def resetCache() = {
    cacheTable.clear()
    cacheKeys.clear()
  }

  def resetCacheForEntity(e:String) = {
    cacheTable.filter(c => CacheUtils.checkIfHasForeignKeys(e, c._2)).foreach{ case (k,_) =>
      cacheTable.remove(k)
    }
  }

  def lookupField(referencingTable:String,lang:String, firstNoPK:Option[String]):String = {

    val lookupLabelFields = BoxConfig.fksLookupLabels

    val default = lookupLabelFields.as[Option[String]]("default").getOrElse("name")

    val myDefaultTableLookupLabelField: String = default match {
      case "firstNoPKField" => firstNoPK.getOrElse("name")
      case JSONUtils.LANG => lang
      case _ => default
    }

    lookupLabelFields.as[Option[String]](referencingTable).getOrElse(myDefaultTableLookupLabelField)
  }


  def of(_schema:String,table:String,lang:String, lookupMaxRows:Int = 100)(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,boxDatabase: FullDatabase,services: Services):Future[JSONMetadata] = boxDatabase.adminDb.run{

    logger.warn("searching cache table for " + Seq(up.name, table, lang, lookupMaxRows).mkString)

    val cacheKey = (up.name, table, lang,lookupMaxRows)

    cacheTable.get(cacheKey) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata table cache miss! cache key: ($table, $lang, $lookupMaxRows), cache: ${cacheTable}")

        val schema = new PgInformationSchema(_schema,table, excludeFields)(ec)

        //    println(schema.fk)

        var constraints = List[String]()

        def field2form(field: PgColumn): DBIO[JSONField] = {
          for {
            fk <- schema.findFk(field.column_name)
            firstNoPK <- fk match {
              case Some(f) => firstNoPKField(_schema,f.referencingTable)
              case None => DBIO.successful(None)
            }
            count <- fk match {
              case Some(fk) => Registry().actions(fk.referencingTable).count().map(_.count)
              case None => DBIO.successful(0)
            }
          } yield {
            fk match {
              case Some(fk) => {
                if (count <= lookupMaxRows) {
                  if (constraints.contains(fk.constraintName)) {
                    logger.info("error: " + fk.constraintName)
                    logger.info(field.column_name)
                    DBIO.successful(JSONField(field.jsonType, name = field.boxName, nullable = !field.required))
                  } else {
                    constraints = fk.constraintName :: constraints //add fk constraint to contraint list


                    val text = lookupField(fk.referencingTable, lang, firstNoPK)

                    val model = fk.referencingTable
                    val value = fk.referencingKeys.head //todo verify for multiple keys


                    import ch.wsl.box.shared.utils.JSONUtils._
                    for {
                      lookupData <- Registry().actions(model).find()
                    } yield {
                      val options = lookupData.map { lookupRow =>
                        JSONLookup(lookupRow.get(value), lookupRow.get(text))
                      }

                      JSONField(
                        field.jsonType,
                        name = field.boxName,
                        nullable = !field.required,
                        placeholder = Some(fk.referencingTable + " Lookup"),
                        widget = Some(WidgetsNames.select),
                        lookup = Some(JSONFieldLookup(model, JSONFieldMap(value, text, field.boxName), options))
                      )
                    }

                  }
                } else { //no lookup from fk
                  DBIO.successful(JSONField(
                    field.jsonType,
                    name = field.boxName,
                    nullable = !field.required,
                    widget = WidgetsNames.defaults.get(field.jsonType)
                  ))
                }
              }
              case _ => DBIO.successful(JSONField(
                field.jsonType,
                name = field.boxName,
                nullable = !field.required,
                widget = WidgetsNames.defaults.get(field.jsonType)
              ))
            }
          }

        }.flatten

        val result = for {
          c <- schema.columns
          fields <- DBIO.from(up.db.run(DBIO.sequence(c.map(field2form))))
          keys <- EntityMetadataFactory.keysOf(_schema,table)
        } yield {

          val keyStrategy = if(Managed(table)) SurrugateKey else NaturalKey

          val fieldList = fields.map(_.name)
          JSONMetadata(
            UUID.randomUUID(),
            table,
            table,
            fields,
            Layout.fromFields(fields),
            table,
            lang,
            fieldList,
            fieldList,
            keys,
            keyStrategy,
            None,
            fieldList,
            None,
            FormActionsMetadata.default
          )
        }


        for{
          metadata <- result
        } yield {
          if(BoxConfig.enableCache) {
            logger.warn("adding to cache table " + Seq(up.name, table, lang, lookupMaxRows).mkString)
            DBIO.successful(cacheTable.put(cacheKey,metadata))
          }
          metadata
        }


      }
    }
  }

  def keysOf(schema:String,table:String)(implicit ec:ExecutionContext):DBIO[Seq[String]] = {
    logger.info("Getting " + table + " keys")
    cacheKeys.get(table) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata keys cache miss! cache key: ($table), cache: ${cacheKeys}")

        val result = new PgInformationSchema(schema,table)(ec).pk.map { pk => //map to enter the future
          logger.info(pk.toString)
          pk.boxKeys
        }


        for{
          keys <- result
        } yield {
          if(BoxConfig.enableCache) {
            DBIO.successful(cacheKeys.put(table,keys))
          }
          keys
        }

      }
    }
  }

  def firstNoPKField(_schema:String,table:String)(implicit db:FullDatabase, ec:ExecutionContext):DBIO[Option[String]] = {
    logger.info("Getting first field of " + table + " that is not PK")
    val schema = new PgInformationSchema(_schema,table,excludeFields)(ec)
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


  def fieldType(table:String,field:String):ColType = {
    val dbField = Registry().fields.field(table,field)
    if(dbField.name != "Unknown") {
      dbField
    } else {
      BoxFieldAccessRegistry.field(table,field)
    }
  }



  def isView(schema:String,table:String)(implicit ec:ExecutionContext):DBIO[Boolean] =
    new PgInformationSchema(schema,table)(ec).pgTable.map(_.isView)  //map to enter the future



}
