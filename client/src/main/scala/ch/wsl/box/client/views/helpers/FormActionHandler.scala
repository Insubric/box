package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.db.{DB, LocalRecord}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{Navigate, Notification, Record}
import ch.wsl.box.model.shared.{FormAction, JSONID, JSONMetadata}
import io.circe.Json
import io.udash.ReadableProperty
import scribe.Logging

import scala.concurrent.Future

class FormActionHandler(
                        metadata:JSONMetadata,
                        val data:ReadableProperty[Json],
                        originalData:Json,
                        public:Boolean,
                        insert:Boolean,
                         _beforeAction: => Unit,
                         _reload: Option[JSONID] => Future[Json],
                         _beforeSave:Boolean => Future[Json],
                         _afterSave:(JSONID,Record) => Unit,
                         _duplicate:Unit => Unit
                       ) extends ActionHandler with Logging {

  import ch.wsl.box.client.Context
  import Context.Implicits._

  override def beforeAction(): Unit = _beforeAction

  override def reload(): Unit = _reload(None)

  override def reload(id: JSONID): Future[Json] = _reload(Some(id))

  override def afterSave(id: JSONID, data: Record): Unit = _afterSave(id,data)

  override def beforeSave(check: Boolean): Future[Json] = _beforeSave(check)

  override def saveToDb(data: Json): Future[(JSONID, Record)] = {

    val originalId = JSONID.fromData(originalData,metadata)
    val id = JSONID.fromData(data,metadata,fullyDefined = true)

    logger.info(s"saveAction id:$originalId $id")
    for {
      result <- id match {
        case Some(id) if !insert => services.data.update (metadata.kind, services.clientSession.lang(), metadata.name, originalId.getOrElse(id), data,public)
        case _ => services.data.insert (metadata.kind, services.clientSession.lang (), metadata.name, data,public)
      }
    } yield {
      logger.debug(s"saveAction::Result")
      (JSONID.fromData(result,metadata,false).getOrElse(JSONID.empty),Record(result,false))
    }
  }

  override def saveLocally(data: Json): Future[(JSONID, Record)] = {

    val originalId = JSONID.fromData(originalData,metadata)

    val id:JSONID = JSONID.fromData(data,metadata,fullyDefined = true) match {
      case Some(value) => value
      case None => JSONID.newLocal()
    }

    val record = LocalRecord(id.asString,metadata.kind,metadata.name,data,Some(originalData))

    logger.info(s"saveAction id:$originalId $id")
    for {
      result <- DB.localRecord.save(record)
    } yield {
      logger.debug(s"saveAction::Result")
      (JSONID.fromString(result.pk.jsonid,metadata).get,Record(result.data,true))
    }
  }


  override def duplicate(): Unit = _duplicate()

  override def delete(id:Option[JSONID],data:Json,write:Boolean,action: FormAction): Unit = {

    for{
      key <- id
    } yield {
      services.data.delete(metadata.kind, services.clientSession.lang(),metadata.name,key).map{ count =>
        Notification.add("Deleted " + count.count + " rows")
        Routes.getUrl(action,data,metadata.kind,metadata.name,Some(key.asString),write) match {
          case Some(value) => Navigate.toUrl(value)
          case None => Navigate.to(Routes(metadata.kind, metadata.name,public).entity(metadata.name))
        }

      }
    }

  }


}
