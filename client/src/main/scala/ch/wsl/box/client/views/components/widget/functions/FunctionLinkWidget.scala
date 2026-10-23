package ch.wsl.box.client.views.components.widget.functions


import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Navigate, Notification}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.Internationalization.I18n
import ch.wsl.box.model.shared.{JSONField, JSONID, NewWindow, Popup, Self, Target, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.{Event, window}
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._

import java.net.URLEncoder


object FunctionLinkWidget extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.functionLink

  override def create(params: WidgetParams): Widget = FunctionLink(params)

  case class FunctionLink(params: WidgetParams) extends Widget {

    import ch.wsl.box.client.Context.Implicits._
    import ch.wsl.box.client.Context._

    import listenerManager._

    override def field: JSONField = params.field



    val buttonStyle = params.field.params.flatMap(_.getOpt("style")) match {
      case Some("Std") => ClientConf.style.boxButton
      case Some("Primary") => ClientConf.style.boxButtonImportant
      case Some("Danger") => ClientConf.style.boxButtonDanger
      case _ => ClientConf.style.boxButton
    }

    val target = params.field.params.flatMap(_.getOpt("target")).map(Target.fromString).getOrElse(NewWindow)

    val label:Modifier = field.icon match {
      case Some(value) => Icons(value)
      case None => field.title
    }



    val clickHandler = { (e: Event) =>

      logger.warn("HELLO")

      val function = field.function.get

      val functionParams = function.local_fields.zip(function.function_fields).map{ case (l,f) =>
        f -> params._allData.get.js(l)
      }

      val url = Routes.function(function.name,Some(Json.fromFields(functionParams)))
      Navigate.toUrl(url,target)

      e.preventDefault()
    }

    override protected def show(nested:Binding.NestedInterceptor) = button(buttonStyle, label).render.click(clickHandler)
    override protected def edit(nested:Binding.NestedInterceptor) = show(nested)

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div(textAlign.center,show(nested))
    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = showOnTable(nested)
  }
}
