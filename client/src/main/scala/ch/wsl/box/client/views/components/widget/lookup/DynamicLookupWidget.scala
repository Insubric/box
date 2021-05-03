package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.Context
import ch.wsl.box.client.services.Notification
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, LookupLabel}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash.properties.single.{Property, ReadableProperty}

trait DynamicLookupWidget extends Widget {

  def params: WidgetParams

  import ch.wsl.box.client.Context._

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

  val monitoredFields:Property[Seq[(String,Json)]] = Property(Seq())

  val remoteField:Property[Json] = Property(Json.Null)

  params.allData.listen({js =>
    val ids:Seq[(String,Json)] = lookupLabel.localIds.zip(lookupLabel.remoteIds).map{ case (localId,remoteId) =>
      remoteId -> js.js(localId)
    }
    monitoredFields.set(ids)
  },true)

  monitoredFields.listen({localFields =>
    if(localFields.exists( x => x._2 != Json.Null)) {
      services.rest.get(
        EntityKind.ENTITY.kind,
        services.clientSession.lang(),
        lookupLabel.remoteEntity,
        JSONID.fromMap(localFields)
      ).map{ remote =>
        val remoteValue = remote.js(lookupLabel.remoteField)
        remoteField.set(remoteValue)
      }
    } else {
      remoteField.set(Json.Null)
    }
  },true)







  def widget() = WidgetRegistry
    .forName(lookupLabel.widget)
    .create(params.copy(prop = remoteField))


}
