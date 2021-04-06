package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, WidgetsNames}
import io.circe.Json
import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import io.udash.properties.single.Property


case class TitleWidget(level:Int) extends ComponentWidgetFactory {

  override def name: String = level match {
    case 1 => WidgetsNames.h1
    case 2 => WidgetsNames.h2
    case 3 => WidgetsNames.h3
    case 4 => WidgetsNames.h4
    case 5 => WidgetsNames.h5
  }


  override def create(params: WidgetParams): Widget = H1WidgetImpl(params.field)

  case class H1WidgetImpl(field:JSONField) extends Widget {

    val _text:String = field.label.getOrElse(field.name)

    override protected def show(): JsDom.all.Modifier = level match {
      case 1 => h1(_text)
      case 2 => h2(_text)
      case 3 => h3(_text)
      case 4 => h4(_text)
      case 5 => h5(_text)
    }

    override protected def edit(): JsDom.all.Modifier = show()
  }
}


