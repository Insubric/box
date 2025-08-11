package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.Child
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.udash.ReadableProperty
import io.udash.properties.single.Property

trait ChildUtils { this:Widget =>

  def widgetParam:WidgetParams

  def child:Child = field.child match {
    case Some(value) => value
    case None => throw new Exception(s" ${field.name} does not have a child")
  }

  private val staticProps:ReadableProperty[Map[String,Json]] = widgetParam.fieldParams match {
    case Some(value) => value.transform{ params =>
      params.jsOpt("props").flatMap(_.asObject).map(_.toMap).getOrElse(Map())
    }
    case None => Property(Map())
  }
//
//    {for{
//    params <- field.params
//    propsJs <- params.jsOpt("props")
//    props <- propsJs.asObject
//  } yield props.toMap}.getOrElse(Map())

  val propagatedFields:ReadableProperty[Json] = widgetParam.allData.combine(staticProps){ case (js,static) =>
    val mapping = for {
      m <- child.mapping
    } yield {
      //      println(s"local:$local sub:$sub")
      m.child ->  widgetParam.allData.get.js(m.parent)
    }

    (child.props.map(p => p -> js.js(p)).toMap ++ static ++ mapping).asJson
  }

}
