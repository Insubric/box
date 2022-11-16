package ch.wsl.box.client.views.components.widget

import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash._
import io.circe._
import io.circe.generic.auto._
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._

case class WidgetSelector(name:String,params:Option[Json])
case class DynamicWidgetOptions(selectorField:String,widgetMapping:Map[String,WidgetSelector],default:WidgetSelector)

object DynamicWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.dynamicWidget


  override def create(params: WidgetParams): Widget = DynamicWidgetImpl(params)

  case class DynamicWidgetImpl(params: WidgetParams) extends Widget {

    override def field: JSONField = params.field

    val opts = params.field.params.getOrElse(Json.Null).as[DynamicWidgetOptions]



    val widgetProperty:ReadableProperty[Widget] = opts match {
      case Left(value) => {
        logger.warn(s"Option not parsable: ${value.message}")
        Property(HiddenWidget.create(params))
      }
      case Right(value) => {
        val watchedField = params.allData.transform(_.get(value.selectorField))
        watchedField.transform{ key =>
          val selector = value.widgetMapping.getOrElse(key,value.default)
          WidgetRegistry.forName(selector.name).create(params.copy(field = params.field.copy(params = Some(params.field.params.getOrElse(Json.obj()).deepMerge(selector.params.getOrElse(Json.obj()))))))
        }
      }
    }

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(produce(widgetProperty){ w => div(w.render(false,Property(true),nested)).render})


    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(bind(params.prop.transform(_.string)))

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = produce(widgetProperty){ w => div(w.render(true,Property(true),nested)).render}
  }
}