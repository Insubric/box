package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
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

    val _text:String = field.label.getOrElse(field.name)

    val clickHandler = { (e: Event) =>
      services.rest.execute(field.function.get,services.clientSession.lang(),params.allData.get).foreach{ _ =>
        applicationInstance.reload()
      }
    }

    override protected def show() = button(ClientConf.style.boxButton, onclick :+= clickHandler, _text)
    override protected def edit() = show()

    override def showOnTable(): JsDom.all.Modifier = a(onclick :+= clickHandler, _text)
    override def editOnTable(): JsDom.all.Modifier = showOnTable()
  }
}