package ch.wsl.box.client.views.components.widget.`trait`

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.utils.StripHtml
import ch.wsl.box.client.views.components.widget.{HasData, Widget}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash._
import scalatags.JsDom.all._

trait HasCounter extends HasParams { this:Widget with HasData =>
  val target = params[Int]("target_length") //field.params.flatMap(_.jsOpt("target_length").flatMap(_.as[Int].toOption))
  val counter = data.combine(target){ case (d,t) =>
    val l = StripHtml(d.string).length
    t match {
      case Some(t) => t - l
      case None => l
    }
  }



  def counterDiv = div(
    paddingLeft := 5.px,
    cls.bindIf(Property(ClientConf.style.error.className.value),
    counter.transform(_ < 0)),
    bind(counter)
  )
}
