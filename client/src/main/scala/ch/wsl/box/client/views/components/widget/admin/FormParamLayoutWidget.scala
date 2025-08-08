package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.views.components.widget.utility.StaticMetadataWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}


object FormParamLayoutWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminFormParamsLayout

  override def create(params: WidgetParams): Widget = StaticMetadataWidget(params)(
    JSONField.integer("maxWidth"),
    JSONField.boolean("hideHeader"),
    JSONField.boolean("hideFooter"),
    JSONField.boolean("mapClosed"),
    JSONField.json("mapStyle").asPopup
  )

}