package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.Context
import ch.wsl.box.client.services.Notification
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, LookupLabel}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash.properties.single.{Property, ReadableProperty}
import scribe.{Logger, Logging}

import scala.collection.immutable.{AbstractSeq, LinearSeq}

trait DynamicLookupWidget extends Widget with Logging {

  def params: WidgetParams

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  override def field: JSONField = params.field

  val lookupLabel: LookupLabel = params.field.lookupLabel match {
    case Some(value) => value
    case None => {
      val message = s"${field.name} has no lookupLabel"
      Notification.add(message)
      logger.warn(message)
      params.field.lookupLabel.get
    }
  }

  val remoteField:Property[Json] = Property(Json.Null)

  override protected def loadWidget(): Unit = {
    logger.debug(s"loadWidget ${params.metadata.name} ${field.name}")
    super.loadWidget()

    var lookupId:Option[String] = None

    params.allData.listen({ js =>
      val ids: Seq[(String, Json)] = lookupLabel.localIds.zip(lookupLabel.remoteIds).map { case (localId, remoteId) =>
        remoteId -> js.js(localId)
      }
      val newId = JSONID.fromMap(ids)
//      logger.debug(s"Listening all data: $newId ${params.metadata.name} ${field.name} curentvale ${remoteField.get}")
      if(!lookupId.contains(newId.asString) || remoteField.get == Json.Null) { // do only if relevant values have changed
        if(newId.valid) {
          lookupId = Some(newId.asString)
          logger.debug(s"Listening: $newId ${params.metadata.name} ${field.name}")

          services.rest.maybeGet(
            EntityKind.ENTITY.kind,
            services.clientSession.lang(),
            lookupLabel.remoteEntity,
            newId,
            params.public
          ).map {
            case Some(remote) => {
              val remoteValue = lookupLabel.remoteField.split(",").toList match {
                case singleField :: Nil => remote.js(singleField)
                case Nil => Json.Null
                case fields => Json.fromString(fields.flatMap(x => remote.getOpt(x)).filterNot(_.isEmpty).mkString(" - "))
              }

              remoteField.set(remoteValue)
            }
            case None => remoteField.set(Json.Null)
          }.recover { case t: Exception =>
            t.printStackTrace()
            remoteField.set(Json.Null)
          }
        } else remoteField.set(Json.Null)
      }
    }, true)


  }


  def widget() = {
    val w = WidgetRegistry
      .forName(lookupLabel.widget)
      .create(params.copy(prop = remoteField))
    w.load()
    w
  }


}
