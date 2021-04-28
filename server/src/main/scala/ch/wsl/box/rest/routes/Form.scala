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
import ch.wsl.box.rest.utils.{Cache, JSONSupport, UserProfile}
import io.circe.Json
import io.circe.parser.parse
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.io.csv.{CSV}
import ch.wsl.box.rest.io.xls.{XLS, XLSExport}
import ch.wsl.box.rest.metadata.{EntityMetadataFactory, MetadataFactory}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by andre on 5/15/2017.
  */
case class Form(
                 name:String,
                 lang:String,
                 jsonActions: String => TableActions[Json], //EntityActionsRegistry().tableActions
                 metadataFactory: MetadataFactory, //JSONFormMetadataFactory(),
                 db:UserDatabase,
                 kind:String,
                 public: Boolean = false,
                 schema:Option[String] = None
               )(implicit up:UserProfile, ec: ExecutionContext, mat:Materializer,services:Services) extends enablers.CSVDownload with Logging {

    import JSONSupport._
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


    implicit val implicitDB = db
    implicit val boxDb = FullDatabase(db,services.connection.adminDB)

    val metadata: DBIO[JSONMetadata] = metadataFactory.of(name,lang)


   private def actions[T](f:FormActions => T):Future[T] = for{
      form <- boxDb.adminDb.run(metadata)
      formActions = FormActions(form,jsonActions,metadataFactory)
    } yield {
      f(formActions)
    }


    private def _tabMetadata(fields:Option[Seq[String]] = None,m:JSONMetadata): Seq[JSONField] = {
        fields match {
          case Some(fields) => m.fields.filter(field => fields.contains(field.name))
          case None => m.fields.filter(field => m.tabularFields.contains(field.name))
        }
    }

  private def viewTableMetadata(fields:Seq[String],tableMetadata:JSONMetadata,viewMetadata:JSONMetadata): Seq[JSONField] = {
    val tableFields = _tabMetadata(Some(fields),tableMetadata)
    val viewFields = _tabMetadata(Some(fields),viewMetadata)
    fields.flatMap{ field =>
      tableFields.find(_.name == field).orElse(viewFields.find(_.name == field))
    }

  }

    def tabularMetadata(fields:Option[Seq[String]] = None) = metadata.flatMap{ m =>
      val filteredFields = m.view match {
        case None => DBIO.successful(_tabMetadata(fields,m))
        case Some(view) => DBIO.from(EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),view,lang).map{ vm =>
          viewTableMetadata(fields.getOrElse(m.tabularFields),m,vm)
        })
      }

      filteredFields.map( ff => m.copy(fields = ff ))

    }

    def privateOnly(r: => Route):Route = {
      if(public) {
        complete(StatusCodes.Unauthorized,"Not authorized to do that action without authentication")
      } else {
        r
      }
    }


  def xls = path("xlsx") {
    get {
      privateOnly {
        parameters('q) { q =>
          val query = parse(q).right.get.as[JSONQuery].right.get
          val io = for {
            metadata <- DBIO.from(boxDb.adminDb.run(tabularMetadata()))
            formActions = FormActions(metadata, jsonActions, metadataFactory)
            fkValues <- Lookup.valuesForEntity(metadata).map(Some(_))
            data <- formActions.list(query, fkValues)
            xlsTable = XLSTable(
              title = name,
              header = metadata.exportFields.map(ef => metadata.fields.find(_.name == ef).map(_.title).getOrElse(ef)),
              rows = data.map(row => metadata.exportFields.map(cell => row.get(cell)))
            )
          } yield {
            XLS.route(xlsTable)
          }
          onSuccess(db.run(io))(x => x)
        }
      }
    }
  }

  def csvTable(query:JSONQuery):Future[CSVTable] = {
    for {
      metadata <- boxDb.adminDb.run(tabularMetadata())
      formActions = FormActions(metadata, jsonActions, metadataFactory)
      csv <- db.run(formActions.csv(query, None))
    } yield csv
  }

  def csvTable(q:String,fk:Option[String],fields:Option[String]):DBIO[CSVTable] = {
    val query = parse(q).right.get.as[JSONQuery].right.get
    val tabMetadata = tabularMetadata(fields.map(_.split(",").map(_.trim).toSeq))
    for {
      metadata <- DBIO.from(boxDb.adminDb.run(tabMetadata))
      formActions = FormActions(metadata, jsonActions, metadataFactory)
      fkValues <- fk match {
        case Some(ExportMode.RESOLVE_FK) => Lookup.valuesForEntity(metadata).map(Some(_))
        case _ => DBIO.successful(None)
      }
      csv <- formActions.csv(query, fkValues, _.exportFields)
    } yield csv.copy(
      showHeader = true,
      header = metadata.exportFields.map(ef => metadata.fields.find(_.name == ef).map(_.title).getOrElse(ef))
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
          onSuccess(db.run(csvTable(q,fk,fields)))(csv => CSV.download(csv))
        }
      }
    }
  }

    def route = pathPrefix("id") {
      path(Segment) { strId =>
        JSONID.fromMultiString(strId) match {
          case ids if ids.nonEmpty =>
            get {
              privateOnly {
                complete(actions { fs =>
                  db.run(fs.getById(ids.head).transactionally).map { record =>
                    logger.info(record.toString)
                    HttpEntity(ContentTypes.`application/json`, record.asJson)
                  }
                })
              }
            } ~
              put {
                privateOnly {
                  entity(as[Json]) { e =>
                    complete {
                      actions { fs =>
                        val values = e.as[Seq[Json]].getOrElse(Seq(e))
                        for {
                          jsonId <- db.run{
                            DBIO.sequence(values.zip(ids).map{ case (x,id) => fs.upsertIfNeeded(Some(id), x)}).transactionally
                          }
                        } yield {
                          if(schema == BoxSchema.schema) {
                              Cache.reset()
                          }
                          if(jsonId.length == 1) jsonId.head.asJson else jsonId.asJson
                        }
                      }
                    }
                  }
                }
              } ~
              delete {
                privateOnly {
                  complete {
                    actions { fs =>
                      for {
                        count <- db.run(DBIO.sequence(ids.map(id => fs.delete(id))).transactionally)
                      } yield JSONCount(count.sum)
                    }
                  }
                }
              }
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
          boxDb.adminDb.run(metadata)
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
          boxDb.adminDb.run(metadata.flatMap(m => new JSONSchemas().of(m)))
        }
      }
    } ~
    path("children") {
      get {
        complete {
          boxDb.adminDb.run(metadata.flatMap{ f => metadataFactory.children(f)})
        }
      }
    } ~
    path("keys") {
      get {
        complete {
          boxDb.adminDb.run(metadata.flatMap(f => EntityMetadataFactory.keysOf(schema.getOrElse(services.connection.dbSchema),f.entity)))
        }
      }
    } ~
    path("ids") {
      privateOnly {
        post {
          entity(as[JSONQuery]) { query =>
            complete {
              for {
                metadata <- boxDb.adminDb.run(tabularMetadata())
                formActions = FormActions(metadata, jsonActions, metadataFactory)
                data <- db.run(formActions.ids(query))
              } yield data
            }
          }
        }
      }
    } ~
    path("count") {
      get {
        complete {
          actions{a =>

            db.run(a.count())

          }
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
                formActions = FormActions(metadata, jsonActions, metadataFactory)
                fkValues <- Lookup.valuesForEntity(metadata).map(Some(_))
                result <- formActions.list(query, fkValues)
              } yield {
                result
              }

              db.run(io)

            }
          }
        }
      }
    } ~
    xls ~
    csv ~
    pathEnd {
        post {
          entity(as[Json]) { e =>
            complete {
              actions{ fs =>
                db.run(fs.insert(e).transactionally)
              }
            }
          }
        }
    }


}
