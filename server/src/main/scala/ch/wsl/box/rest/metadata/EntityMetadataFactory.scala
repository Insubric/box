package ch.wsl.box.rest.metadata

import java.util.UUID
import akka.stream.Materializer
import ch.wsl.box.information_schema.{PgColumn, PgInformationSchema}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.shared.utils.JSONUtils
import com.typesafe.config.Config
import scribe.Logging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{FullDatabase, Managed, TypeMapping}
import ch.wsl.box.rest.runtime.{ColType, RegistryInstance}
import ch.wsl.box.services.Services
import io.circe.Json

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try


object EntityMetadataFactory extends Logging {

  private case class SchemaCache(
                          cacheTable:scala.collection.mutable.Map[String, JSONMetadata],
                          cacheKeys: scala.collection.mutable.Map[String, Seq[String]]
  )

  val excludeFields:Seq[String] = Try{
    com.typesafe.config.ConfigFactory.load().as[Seq[String]]("db.generator.excludeFields")
  }.getOrElse(Seq())
  private val schemaCache = scala.collection.mutable.Map[String,SchemaCache]()
  private def getSchema(schema:String):SchemaCache = {
    schemaCache.getOrElse(schema,{
      val newSchemaCache = SchemaCache(scala.collection.mutable.Map[String, JSONMetadata](),scala.collection.mutable.Map[String, Seq[String]]())
      schemaCache.put(schema,newSchemaCache)
      newSchemaCache
    })
  }

  def resetCache(schema:String) = {
    schemaCache.get(schema).foreach { s =>
      s.cacheTable.clear()
      s.cacheKeys.clear()
    }

  }


  def lookupField(registry:RegistryInstance, referencingTable:String, firstNoPK:Option[String],valueField:Seq[String])(implicit services: Services):Seq[String] = {

    val lookupLabelFields = services.config.fksLookupLabels

    val default = lookupLabelFields.as[Option[String]]("default").map(x => Seq(x)).getOrElse(valueField)

    val myDefaultTableLookupLabelField: Seq[String] = default match {
      case "firstNoPKField" => firstNoPK.map(x => Seq(x)).getOrElse(valueField)
      case _ => default
    }

    val maybeField = lookupLabelFields.as[Option[String]](referencingTable).map(x => Seq(x)).getOrElse(myDefaultTableLookupLabelField)

    val existingFields = maybeField.filter(f => registry.fields.field(referencingTable,f).isDefined )
    if(existingFields.nonEmpty) existingFields else valueField
  }


  def of(table:String,registry:RegistryInstance)(implicit up:UserProfile, ec:ExecutionContext,boxDatabase: FullDatabase,services: Services):Future[JSONMetadata] = boxDatabase.adminDb.run{

    val cacheKey = (registry.schema, table)

    logger.info(s"searching cache table for $cacheKey")



    getSchema(registry.schema).cacheTable.get(table) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.warn(s"Metadata table cache miss! cache key: $cacheKey")

        val schema = new PgInformationSchema(registry.schema,table, excludeFields)(ec)

        //    println(schema.fk)

        var constraints = List[String]()

        def field2form(field: PgColumn): DBIO[JSONField] = {
          for {
            fk <- schema.findFk(field.column_name)
            firstNoPK <- fk match {
              case Some(f) => firstNoPKField(registry.schema,f.referencingTable)
              case None => DBIO.successful(None)
            }
          } yield {
            fk match {
              case Some(fk) => {


                  val foreignValue = fk.referencingKeys(fk.keys.indexOf(field.column_name))

                  val model = fk.referencingTable

                  val text = lookupField(registry,model, firstNoPK, fk.referencingKeys)


                  JSONField(
                    field.jsonType,
                    name = field.boxName,
                    nullable = !field.required,
                    placeholder = Some(fk.referencingTable + " Lookup"),
                    widget = Some(WidgetsNames.select),
                    lookup = Some(JSONFieldLookupRemote(model, JSONFieldMap(JSONFieldMapForeign(foreignValue,fk.referencingKeys,text), fk.keys)))
                  )



              }
              case _ => JSONField(
                field.jsonType,
                name = field.boxName,
                nullable = !field.required,
                widget = WidgetsNames.defaults.get(field.jsonType)
              )
            }
          }

        }

        val result = for {
          c <- schema.columns
          fields <- DBIO.from(up.db.run(DBIO.sequence(c.map(field2form))))
          keys <- EntityMetadataFactory.keysOf(registry.schema,table)
        } yield {

          val keyStrategy = if(Managed(table)) SurrugateKey else NaturalKey

          val fieldList = fields.map(_.name)
          JSONMetadata(
            UUID.randomUUID(),
            table,
            EntityKind.ENTITY.kind,
            table,
            fields,
            Layout.fromFields(fields),
            table,
            "",
            fieldList,
            fieldList,
            keys,
            keyStrategy,
            None,
            fieldList,
            None,
            FormActionsMetadata.default,
            params = Some(Json.fromFields(Map("maxWidth" -> Json.fromInt(800), "hideFooter" -> Json.True)))
          )
        }


        for{
          metadata <- result
        } yield {
          if(services.config.enableCache) {
            logger.warn("adding to cache table " + cacheKey)
            DBIO.successful(getSchema(registry.schema).cacheTable.put(table,metadata))
          }
          metadata
        }


      }
    }
  }

  def keysOf(schema:String,table:String)(implicit ec:ExecutionContext,services:Services):DBIO[Seq[String]] = {

    val cacheKey = (schema,table)
    logger.info("Getting " + cacheKey + " keys")
    getSchema(schema).cacheKeys.get(table) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata keys cache miss! cache key: ($table)")

        val result = new PgInformationSchema(schema,table)(ec).pk.map { pk => //map to enter the future
          logger.info(pk.toString)
          pk.boxKeys
        }


        for{
          keys <- result
        } yield {
          if(services.config.enableCache) {
            DBIO.successful(getSchema(schema).cacheKeys.put(table,keys))
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
      c.map(_.column_name).diff(pks).headOption
    }
  }


  def fieldType(table:String,field:String,registry:RegistryInstance):Option[ColType] = {
    registry.fields.field(table,field)
  }


  def isView(schema:String,table:String)(implicit ec:ExecutionContext):DBIO[Boolean] =
    new PgInformationSchema(schema,table)(ec).pgTable.map(_.isView)  //map to enter the future



}
