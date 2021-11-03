package ch.wsl.box.rest.html


import io.circe.Json
import org.jsoup.Jsoup

import scala.concurrent.{ExecutionContext, Future}

trait Html{
  def render(html:String,json:Json)(implicit ex:ExecutionContext):Future[String]

  def stripTags(html:String):String = {
    val doc = Jsoup.parse(html)
    doc.text()
  }
}

object Html extends Html {

  private val renderer = new mustache.Mustache()

  def render(html:String,json:Json)(implicit ex:ExecutionContext) = renderer.render(html,json)



}
