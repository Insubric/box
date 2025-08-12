package ch.wsl.box.client.views.components.ui

import ch.wsl.box.model.shared.GeoJson
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import io.circe.parser._
import io.circe.scalajs._
import io.udash.properties.single.Property

import scala.scalajs.js.JSConverters._
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Event, HTMLDivElement, HTMLInputElement, MutationObserver, MutationObserverInit, document}
import scalatags.JsDom.all._
import scribe.Logging
import ch.wsl.typings.autocompleter
import ch.wsl.typings.autocompleter.{autocompleterBooleans, mod}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.{JSON, UndefOr, |}


object Autocomplete extends Logging {
  def apply[T](prop: Property[Option[T]], fetch: js.Function1[String,Future[Seq[T]]], toLabel: js.Function1[Option[T],String], toSuggestion: js.Function1[T,HTMLDivElement])(modifier: Modifier*)(implicit ec: ExecutionContext, enc:Encoder[T], dec:Decoder[T]): HTMLInputElement = {
    val el: HTMLInputElement = input(modifier).render

    prop.listen({ item => el.value = toLabel(item) }, true)



    val observer = new MutationObserver({ (mutations, observer) =>
      if (document.contains(el)) {
        observer.disconnect()

        val options = new js.Object {
          val input = el

          val debounceWaitMs = 200

          val preventSubmit = true

          @JSName("fetch")
          def fetch_false(text: String, update: js.Function1[js.Array[String] | autocompleterBooleans.`false`, Unit], trigger: mod.EventTrigger, cursorPos: Double): Unit = {
            logger.info(s"Fetching $text")
            fetch(text).map { data =>
              val dataJS: js.Array[String] = data.map(x => x.asJson.noSpaces).toJSArray
              logger.info(s"Got $dataJS")
              update(dataJS)
            }
          }


          val render:UndefOr[js.Function3[String,String,Int,UndefOr[Div]]] = js.defined{ (item:String,_,_) =>

            logger.info(s"Render $item")

            def error(msg:String) = {
              logger.warn(s"Parsing error $value json: $item")
              Some(div("").render).orUndefined
            }

            val result = parse(item).flatMap(_.as[T]) match {
              case Left(value) => error(value.toString)
              case Right(value) => {
                    logger.info(value.toString())
                    Some(toSuggestion(value)).orUndefined

              }
            }
            result
          }

          def onSelect(item: String, input: HTMLInputElement): Unit = {
            parse(item).flatMap(_.as[T]) match {
              case Left(value) => logger.warn(s"Parsing error $value json: $item")
              case Right(value) => prop.set(Some(value))
            }
          }
        }
        autocompleter.mod.^.asInstanceOf[js.Dynamic](options)
      }
    })

    observer.observe(document, MutationObserverInit(childList = true, subtree = true))

    el

  }
}
