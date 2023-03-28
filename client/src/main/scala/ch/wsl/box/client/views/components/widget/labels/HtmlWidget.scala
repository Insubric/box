package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.routes.Routes
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


    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = template match {
      case Left(_) => raw(_text)
      case Right(tmpl) => {

        val renderer = mustache.render(tmpl)

        val variables = tmpl.els.flatMap{
          case Variable(key, _) => Some(key)
          case Section(key, _, _) => Some(key)
          case _ => None
        }

        val watchedVariables:ReadableProperty[Context] = data.transform{ js =>
          val values = variables.map{v =>
            v -> js.js(v).toMustacheValue
          } ++ Seq(
            "BASE_URI" -> Value.of(Routes.baseUri),
            "FULL_URL" -> Value.of(Routes.fullUrl),
            "ORIGIN_URL" -> Value.of(Routes.originUrl)
          )
          Context(values:_*)
        }


        nested(produce(watchedVariables) { context =>
          raw(renderer(context)).render
        })

      }
    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)

    override def showOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)

    override def editOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)
  }
}