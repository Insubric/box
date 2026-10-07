package ch.wsl.box.client.views.components.widget.helpers

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.StyleConf
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.fonts.BoxFont
import ch.wsl.box.client.styles.utils.ColorUtils
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.child.TableStyle
import io.circe.Json
import org.scalajs.dom.{Event, document}
import scribe.Logging
import io.udash._
import io.udash.css.CssView._
import scalacss.ScalatagsCss._
import io.circe._
import io.circe.generic.auto._
import scalacss.internal.mutable.StyleSheet

case class Param(style:String,color:Option[String],background:Option[String],buttonStyle:Option[String])

import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._

case class LinkStyle(mainColor:String,linkColor:String,font:BoxFont) extends StyleSheet.Inline {
  import dsl._


  val boxedLink = style(
    font.bold,
    width(120 px),
    height(120 px),
    margin(20 px),
    padding(10 px),
    display.flex,
    flexDirection.column,
    alignItems.center,
    justifyContent.center,
    textAlign.center,
    gap(8.px),
    border.none,
    borderRadius(2.px),
    background := s"linear-gradient(135deg, ${ColorUtils.RGB.fromHex(mainColor).lighten(100).toHex} 0%, ${mainColor} 100%)",
    color := linkColor,
    fontSize(12.px),
    textTransform.uppercase,
    cursor.pointer,
    transition := "background 0.3s ease, box-shadow 0.3s ease",
    boxShadow := "0 2px 8px rgba(0, 0, 0, 0.2)",
    position.relative,
    overflow.hidden,

    &.hover(
      background := s"linear-gradient(135deg, ${ColorUtils.RGB.fromHex(mainColor).darken(10).toHex} 0%, ${mainColor} 100%)",
      boxShadow := "0 4px 12px rgba(42, 42, 42, 0.3)"
    ),

    &.active(
      boxShadow := "0 1px 4px rgba(0, 0, 0, 0.15)"
    ),

    unsafeChild("svg")(
      width(36.px),
      height(36.px),
      position.relative,
      zIndex(1)
    )
  )

}

trait Link extends Logging {

  import scalatags.JsDom.all._

  def linkRenderer(label:Modifier, params:Option[Json],click: (Event) => Any):Modifier = {
    val linkParam = params.flatMap(_.as[Param] match {
      case Left(value) => {
        logger.warn(value.message)
        None
      }
      case Right(value) =>Some(value)
    })

    logger.info(s"Param: $linkParam, json ${params}")

    linkParam match {
      case Some(Param(style,_color,background,_)) if style == "box" => {

        val linkStyle = LinkStyle(background.getOrElse(ClientConf.colorMain),_color.getOrElse("white"),services.style.font())
        val linkStyleElement = document.createElement("style")
        linkStyleElement.innerText = linkStyle.render(cssStringRenderer, cssEnv)

        Seq[Modifier](
          linkStyleElement,
          a(
            id := TestHooks.linkedFormButton(label.toString),
            onclick :+= click,
            div(linkStyle.boxedLink,
              label
            )
          )
        )
      }
      case Some(Param(style,_,_,bs)) if style == "button" => {
        val buttonStyle = bs match {
          case Some("Std") => ClientConf.style.boxButton
          case Some("Primary") => ClientConf.style.boxButtonImportant
          case Some("Danger") => ClientConf.style.boxButtonDanger
          case _ => ClientConf.style.boxButton
        }
        button(buttonStyle, onclick :+= click, label)
      }
      case _ =>  a(label, onclick :+= click)
    }

  }
}
