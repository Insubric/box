package ch.wsl.box.client.views.components.widget.functions

import ch.wsl.box.client.services.{ClientConf, Notification}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.Internationalization.I18n
import ch.wsl.box.model.shared.{JSONField, JSONID, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.{Event, window}
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._


object ExecuteFunctionWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.executeFunction

  override def create(params: WidgetParams): Widget = ExecuteFunctionWidgetImpl(params)

  case class ExecuteFunctionWidgetImpl(params: WidgetParams) extends Widget {

    import ch.wsl.box.client.Context.Implicits._
    import ch.wsl.box.client.Context._

    override def field: JSONField = params.field

    val saveBefore = params.field.params.exists(_.js("saveBefore") == Json.True)
    val noReload = params.field.params.exists(_.js("noReload") == Json.True)
    val confirmText:Option[String] = {
        val ct = params.field.params.flatMap(_.jsOpt("confirm"))
        ct.flatMap(_.as[I18n].toOption) match {
          case Some(value) => value.find(_.lang == services.clientSession.lang()).orElse(value.headOption).map(_.label)
          case None => ct.flatMap(_.asString)
        }
    }

    val buttonStyle = params.field.params.flatMap(_.getOpt("style")) match {
      case Some("Std") => ClientConf.style.boxButton
      case Some("Primary") => ClientConf.style.boxButtonImportant
      case Some("Danger") => ClientConf.style.boxButtonDanger
      case _ => ClientConf.style.boxButton
    }

    val _text:String = field.label.getOrElse(field.name)


    val clickHandler = { (e: Event) =>

      def exec() = {
        val allData = params.allData.get
        val id = params.id.get.flatMap(id => JSONID.fromString(id,params.metadata))
        services.clientSession.loading.set(true)
        logger.info(s"Exec with params $allData")
        services.rest.execute(field.function.get.name, services.clientSession.lang(), allData).map { result =>
          services.clientSession.loading.set(false)
          result.errorMessage.foreach(Notification.add)
          if(!noReload)
            id.foreach(params.actions.reload)
        }
      }
      def action() = saveBefore match {
        case true => params.actions.save(true).map(_ => exec())
        case false => exec()
      }

      confirmText match {
        case Some(value) => if(window.confirm(value)) action()
        case None => action()
      }

      e.preventDefault()
    }

    override protected def show(nested:Binding.NestedInterceptor) = button(buttonStyle, onclick :+= clickHandler, _text)
    override protected def edit(nested:Binding.NestedInterceptor) = show(nested)

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div(textAlign.center,show(nested))
    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = showOnTable(nested)
  }
}