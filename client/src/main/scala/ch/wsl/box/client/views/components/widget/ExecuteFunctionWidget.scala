package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{ClientConf, Notification}
import ch.wsl.box.model.shared.{JSONField, JSONID, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom.Event
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.css.CssView._
import io.udash._
import io.udash.css._
import scalacss.ScalatagsCss._


object ExecuteFunctionWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.executeFunction

  override def create(params: WidgetParams): Widget = ExecuteFunctionWidgetImpl(params)

  case class ExecuteFunctionWidgetImpl(params: WidgetParams) extends Widget {

    import ch.wsl.box.client.Context._

    override def field: JSONField = params.field

    val saveBefore = params.field.params.exists(_.js("saveBefore") == Json.True)

    val _text:String = field.label.getOrElse(field.name)


    val clickHandler = { (e: Event) =>

      def exec(id:Option[JSONID],allData:Json) = {
        services.clientSession.loading.set(true)
        logger.info(s"Exec with params $allData")
        services.rest.execute(field.function.get, services.clientSession.lang(), allData).foreach { result =>
          services.clientSession.loading.set(false)
          result.errorMessage.foreach(Notification.add)
          id.foreach(params.actions.reload)
        }
      }
      saveBefore match {
        case true => params.actions.save().map{ case (id,data) =>
          exec(Some(id),data)
        }
        case false => exec(None,params.allData.get)
      }
      e.preventDefault()
    }

    override protected def show() = button(ClientConf.style.boxButton, onclick :+= clickHandler, _text)
    override protected def edit() = show()

    override def showOnTable(): JsDom.all.Modifier = a(onclick :+= clickHandler, _text)
    override def editOnTable(): JsDom.all.Modifier = showOnTable()
  }
}