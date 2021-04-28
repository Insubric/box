package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.views.components.widget.lookup.DynamicLookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.properties.single.Property
import scalatags.JsDom
import scalatags.JsDom.all._


object LookupLabelWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.lookupLabel


  override def create(params: WidgetParams): Widget = LookupLabelImpl(params)

  case class LookupLabelImpl(params: WidgetParams) extends DynamicLookupWidget {

    remoteField.listen(js =>
      params.allData.set(params.allData.get.deepMerge(Json.obj(params.field.name -> js)))
    )

    override protected def show(): JsDom.all.Modifier = {
      div(
        widget().render(false,Property(true))
      )
    }



    override protected def edit(): JsDom.all.Modifier = show()
  }
}