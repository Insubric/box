package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.services.Labels
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import io.circe.Json
import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._


object StaticTextWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.staticText


  override def create(params: WidgetParams): Widget = StaticTextWidgetImpl(params.field)

  case class StaticTextWidgetImpl(field:JSONField) extends Widget {

    val _text:String = field.label.getOrElse(Labels(field.name))

    override protected def show(): JsDom.all.Modifier = p(_text)


    override protected def edit(): JsDom.all.Modifier = show()
  }
}