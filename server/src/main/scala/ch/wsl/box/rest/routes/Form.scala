package ch.wsl.box.rest.routes

import java.io.ByteArrayOutputStream
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import ch.wsl.box.jdbc.{Connection, FullDatabase, UserDatabase}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.utils.{BoxSession, Cache, JSONSupport, Lang, UserProfile}
import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.io.csv.CSV
import ch.wsl.box.rest.io.geotools.{GeoPackageWriter, ShapeFileWriter}
import ch.wsl.box.rest.io.xls.{XLS, XLSExport}
import ch.wsl.box.rest.logic.functions.PSQLImpl
import ch.wsl.box.rest.metadata.{EntityMetadataFactory, MetadataFactory}
import ch.wsl.box.rest.runtime.{Registry, RegistryInstance}
import ch.wsl.box.services.Services

import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by andre on 5/15/2017.
  */
case class Form(
                 name:String,
                 lang:String,
                 registry: RegistryInstance,
                 metadataFactory: MetadataFactory,
                 kind:String,
                 show_list: Boolean = true
               )(implicit session:BoxSession, val ec: ExecutionContext, val mat:Materializer, val services:Services) extends enablers.CSVDownload with Logging with HasLookup[Json] with Exporters {

    import JSONSupport._
    import Light._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.server.Directives._

    import io.circe.generic.extras.auto._
    import io.circe.generic.extras.Configuration
    implicit val customConfig: Configuration = Configuration.default.withDefaults

    import io.circe.syntax._
    import ch.wsl.box.shared.utils.Formatters._ //need to be after circe generic auto or it will be overridden
    import ch.wsl.box.shared.utils.JSONUtils._
    import ch.wsl.box.model.shared.EntityKind
    import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

    implicit val l = Lang(lang)
    implicit val up = session.userProfile
    val db = up.db
    implicit val implicitDB = db
    implicit val boxDb = FullDatabase(db,services.connection.adminDB)

    def metadata: JSONMetadata = Await.result(boxDb.adminDb.run(metadataFactory.of(name,lang,session.user)),10.seconds)
   private def actions:FormActions = FormActions(metadata,registry,metadataFactory)

  private def _tabMetadata(fields:Option[Seq[String]] = None,m:JSONMetadata): Seq[JSONField] = {
        fields match {
          case Some(fields) => m.fields.filter(field => fields.contains(field.name))
          case None => m.fields.filter(field => m.tabularFields.contains(field.name) || m.exportFields.contains(field.name))
        }
    }

  private def viewTableMetadata(fields:Seq[String],tableMetadata:JSONMetadata,viewMetadata:JSONMetadata): Seq[JSONField] = {
    val tableFields = _tabMetadata(Some(fields),tableMetadata)
    val viewFields = _tabMetadata(Some(fields),viewMetadata)
    fields.flatMap{ field =>
      tableFields.find(_.name == field).orElse(viewFields.find(_.name == field))
    }

  }

    def tabularMetadata(fields:Option[Seq[String]] = None): DBIO[JSONMetadata] = {
      val filteredFields = metadata.view match {
        case None => DBIO.successful(_tabMetadata(fields,metadata))
        case Some(view) => DBIO.from(EntityMetadataFactory.of(view,registry).map{ vm =>
          viewTableMetadata(fields.getOrElse(metadata.tabularFields),metadata,vm)
        })
      }

      filteredFields.map( ff => metadata.copy(fields = ff ))

    }

    def privateOnly(r: => Route):Route = {
      if(!show_list) {
        complete(StatusCodes.Unauthorized,"Not authorized to do that action without authentication")
      } else {
        r
      }
    }


  def csvTable(query:JSONQuery):Future[CSVTable] = {
    for {
      metadata <- boxDb.adminDb.run(tabularMetadata())
      formActions = FormActions(metadata, registry, metadataFactory)
      csv <- db.run(formActions.csv(query, false))
    } yield csv
  }

  def csvTable(q:String,fk:Option[String],fields:Option[String]):Future[CSVTable] = {
    val query = parse(q).right.get.as[JSONQuery].right.get
    //val formActions = FormActions(metadata, registry, metadataFactory)
    for {
      metadata <- boxDb.adminDb.run(tabularMetadata())
      formActions = FormActions(metadata, registry, metadataFactory)
      csv <- db.run(formActions.csv(query, true, _.exportFieldsNoGeom.map(_.name)))
    } yield csv.copy(
      showHeader = true,
      header = metadata.exportFieldsNoGeom.map(_.title)
    )
  }

  def csv:Route = path("csv") {
    post {
      privateOnly {
        entity(as[JSONQuery]) { query =>
          onSuccess(csvTable(query))(csv => CSV.body(csv))
        }
      }
    } ~ get {
      privateOnly {
        parameters('q, 'fk.?, 'fields.?) { (q, fk, fields) =>
          onSuccess(csvTable(q,fk,fields))(csv => CSV.download(csv))
        }
      }
    }
  }

  def resetCacheOnBox() = if(registry.schema == Registry.box().schema) {
    Cache.reset()
  }

  def updateDiff = put {
      entity(as[JSONDiff]) { e =>
        complete {
          for {
            json <- db.run{
              actions.updateDiff(e).transactionally
            }
          } yield {
            resetCacheOnBox()
            json
          }
        }
      }
  }


  def _get(ids:Seq[JSONID]) = get {

      complete({
        db.run(actions.getById(ids.head).transactionally).map { record =>
          logger.info(record.toString)
          HttpEntity(ContentTypes.`application/json`, record.asJson)
        }
      })

  }

  def _delete(ids:Seq[JSONID]) = delete {
      complete {

          val action = DBIO.sequence(ids.map(id => actions.delete(id))).transactionally
          //println(action.statements.mkString("\n"))
          for {
            count <- db.run(action)
          } yield JSONCount(count.sum)

      }
  }

  def update(ids:Seq[JSONID]) = put {
      entity(as[Json]) { e =>
        complete {

            val values = e.as[Seq[Json]].getOrElse(Seq(e))
            val result = for {
              jsonId <- db.run{
                DBIO.sequence(values.zip(ids).map{ case (x,id) => actions.update(id, x)}).transactionally
              }
            } yield {
              resetCacheOnBox()
              if(jsonId.length == 1) jsonId.head.asJson else jsonId.asJson
            }

            result.recover{ case t:Throwable => t.printStackTrace()}

            result

        }
      }
  }


  def route = pathPrefix("id") {
      path(Segment) { strId =>
        JSONID.fromMultiString(strId,metadata) match {
          case ids if ids.nonEmpty =>
              _get(ids) ~
              update(ids) ~
              _delete(ids)
          case _ => complete(StatusCodes.BadRequest,s"JSONID $strId not valid")
        }
      }
    } ~
    path("kind") {
      get {
        complete{kind}
      }
    } ~
    path("metadata") {
      get {
        complete {
          metadata
        }
      }
    } ~
    path("tabularMetadata") {
      get {
        complete {
          boxDb.adminDb.run(tabularMetadata())
        }
      }
    } ~
    path("schema") {
      get {
        complete {
          boxDb.adminDb.run(new JSONSchemas().of(metadata))
        }
      }
    } ~
    path("children") {
      get {
        complete {
          boxDb.adminDb.run(metadataFactory.children(metadata,session.user))
        }
      }
    } ~
    path("keys") {
      get {
        complete {
          boxDb.adminDb.run( EntityMetadataFactory.keysOf(registry.schema,metadata.entity))
        }
      }
    } ~
    path("ids") {
      privateOnly {
        post {
          entity(as[JSONQuery]) { query =>
            complete {
              for {
                data <- db.run(actions.ids(query,metadata.keys))
              } yield data
            }
          }
        }
      }
    } ~
    path("count") {
      get {
        complete {
            db.run(actions.count())
        }
      }
    } ~
    path("list") {
      post {
        privateOnly {
          entity(as[JSONQuery]) { query =>
            logger.info("list")
            complete {
              val io = for {
                metadata <- DBIO.from(boxDb.adminDb.run(tabularMetadata()))
                formActions = FormActions(metadata, registry, metadataFactory)
                result <- formActions.list(query, true)
              } yield {
                result
              }

              db.run(io)

            }
          }
        }
      }
    } ~
    lookup(Future.successful(metadata)) ~
    lookups(actions) ~
    pathEnd {
      post {
        entity(as[Json]) { e =>
          complete {
            db.run(actions.insert(e).transactionally)
          }
        }
      }
    } ~
    privateOnly {
      xls ~
      csv ~
      shp ~
      geoPkg ~
      GeoData(db, actions)
    }



}
