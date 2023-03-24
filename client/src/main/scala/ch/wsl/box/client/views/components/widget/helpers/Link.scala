package ch.wsl.box.client.views.components.widget.helpers

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.utils.TestHooks
import io.circe.Json
import org.scalajs.dom.Event
import scalatags.JsDom.all._
import scribe.Logging
import io.udash._
import io.udash.css.CssView._
import scalacss.ScalatagsCss._
import io.circe._
import io.circe.generic.auto._

case class Param(style:String,color:Option[String],background:Option[String])

trait Link extends Logging {

  def linkRenderer(label:Modifier, params:Option[Json],click: (Event) => Any) = {
    val linkParam = params.flatMap(_.as[Param] match {
      case Left(value) => {
        logger.warn(value.message)
        None
      }
      case Right(value) =>Some(value)
    })

    logger.info(s"Param: $linkParam, json ${params}")

    linkParam match {
      case Some(Param(style,_color,background)) if style == "box" => {
        a(
          id := TestHooks.linkedFormButton(label.toString),
          onclick :+= click,
          div(ClientConf.style.boxedLink,
            color := _color.getOrElse(ClientConf.colorLink),
            backgroundColor := background.getOrElse(ClientConf.colorMain),
            label
          )
        )
      }
      case _ =>  a(label, onclick :+= click)
    }

  }
}
