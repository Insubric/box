package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash._
import scalatags.JsDom
import scalatags.JsDom.all._
import yamusca.imports._
import io.circe._

object HtmlWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.html


  override def create(params: WidgetParams): Widget = HtmlWidgetImpl(params.field,params.allData)

  case class HtmlWidgetImpl(field:JSONField,data:Property[Json]) extends Widget {


    val text:String = field.label.getOrElse(field.name)

    val template = mustache.parse(text)


    override protected def show(): JsDom.all.Modifier = template match {
      case Left(_) => raw(text)
      case Right(tmpl) => {

        val renderer = mustache.render(tmpl)

        val variables = tmpl.els.flatMap{
          case Variable(key, _) => Some(key)
          case _ => None
        }
        val watchedVariables:ReadableProperty[Context] = data.transform{ js =>
          val values = variables.map{v =>
            v -> js.js(v).toMustacheValue
          }
          Context(values:_*)
        }


        autoRelease(produce(watchedVariables) { context =>
          raw(renderer(context)).render
        })

      }
    }


    override protected def edit(): JsDom.all.Modifier = show()
  }
}