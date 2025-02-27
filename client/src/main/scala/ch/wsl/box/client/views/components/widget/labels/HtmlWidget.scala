package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.utils.MustacheUtils
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash._
import scalatags.JsDom
import scalatags.JsDom.all._
import yamusca.imports._
import io.circe._
import io.udash.bindings.modifiers.Binding

object HtmlWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.html


  override def create(params: WidgetParams): Widget = HtmlWidgetImpl(params.field,params.allData)

  case class HtmlWidgetImpl(field:JSONField,data:ReadableProperty[Json]) extends Widget {


    val _text:String = field.label.getOrElse(field.name)

    val template = mustache.parse(_text)
    val variables = template.toOption.map(MustacheUtils.extractVariables)


    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = template match {
      case Left(_) => raw(_text)
      case Right(tmpl) => {

        val renderer = mustache.render(tmpl)


        val watchedVariables:ReadableProperty[Seq[(String,Json)]] = data.transform{ js =>
          MustacheUtils.variables(variables.get,js)
        }


        nested(produce(watchedVariables) { context =>
          raw(renderer(MustacheUtils.context(context))).render
        })

      }
    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)

    override def showOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)

    override def editOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)
  }
}