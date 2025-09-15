package ch.wsl.box.client.views.components.widget.utility

import ch.wsl.box.client.views.components.JSONMetadataRenderer
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata}
import io.udash.Property
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scribe.Logging

import java.util.UUID



case class StaticMetadataWidget(params:WidgetParams)(fields:JSONField*) extends Widget with Logging {

  import ch.wsl.box.client.Context._

  override def field: JSONField = params.field

  override protected def show(nested: Binding.NestedInterceptor): JsDom.all.Modifier = edit(nested)

  val metadata = JSONMetadata.simple(UUID.randomUUID(),"internal","none",services.clientSession.lang(),fields,Seq())

  override protected def edit(nested: Binding.NestedInterceptor): JsDom.all.Modifier = JSONMetadataRenderer(metadata,params.prop,Seq(),params.id,params.actions,Property(false),false).edit(nested)
}