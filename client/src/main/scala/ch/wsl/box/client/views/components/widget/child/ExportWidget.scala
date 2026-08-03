package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.views.components.table.{ExportParams, ExportTableDialog}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONQuery, JSONQueryFilter, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._



object ExportWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.`export`

  override def create(params: WidgetParams): Widget = ExportWidgetImpl(params)

  case class ExportWidgetImpl(params: WidgetParams) extends Widget {

    val exportDialog = new ExportTableDialog

    override def field: JSONField = params.field


    def exportParams = for{
      c <- field.child
      m <- params.children.find(_.objId == c.objId)
    } yield {
      val childFilters = c.mapping.map(m => JSONQueryFilter.WHERE.eq(m.child,params.allData.get.get(m.parent)))
      ExportParams(m,m.table,c.childQuery.getOrElse(JSONQuery.empty).filterWith(childFilters:_*))
    }

    override def killWidget(): Unit = {
      super.killWidget()
      exportDialog.clean()
    }

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      exportParams match {
        case Some(ep) => div(
          exportDialog.render(nested,() => ep)
        )
        case None => div("Please check form definition, can't render the export")
      }

    }

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)
  }
}
