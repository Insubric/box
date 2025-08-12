package ch.wsl.box.client.routes

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.utils.{Base64, MustacheUtils}
import ch.wsl.box.client.{EntityFormState, EntityTableState, RoutingState}
import ch.wsl.box.model.shared.{FormAction, JSONFieldLookupRemote, JSONQuery}
import org.scalajs.dom
import org.scalajs.dom.{URLSearchParams, window}
import scribe.Logging
import yamusca.imports.mustache

import scala.scalajs.js
import scala.scalajs.js.URIUtils
import ch.wsl.box.client.utils.Base64._

/**
  * Created by andre on 6/6/2017.
  */

trait Routes{
  def add():RoutingState
  def edit(id:String):RoutingState
  def show(id:String):RoutingState
  def entity(query:Option[JSONQuery]):RoutingState
  def entity(name:String):RoutingState
}

object Routes extends Logging {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._

  def urlParams = {
    new URLSearchParams(dom.window.location.search).map(x => x._1 -> x._2).toMap
  }

  def fullUrl = dom.document.asInstanceOf[js.Dynamic].baseURI.asInstanceOf[String]
  def originUrl = dom.window.location.origin

  def baseUri = {
    val bu = fullUrl
    if(bu.contains("//")) {
      bu.split("/").drop(3).toList match {
        case Nil => "/"
        case x => x.mkString("/","/","/")
      }
    } else {
      bu
    }
  }

  def removeBase(s:String):String = {
    val result = s.stripPrefix(baseUri.stripSuffix("/"))
    logger.info(s"Original $s result: $result base $baseUri")
    result
  }


  def apiV1(path:String = ""):String = {
    baseUri + "api/v1"+path
  }

  def wsV1(topic:String):String = {
    fullUrl.replace("http","ws") + "api/v1/notifications/"+topic
  }

  def apply(kind:String, entityName:String,public:Boolean) = new Routes{
    def add() = EntityFormState(kind,entityName,"true",None,public)
    def edit(id:String) = EntityFormState(kind,entityName,"true",Some(id),public)
    def show(id:String) = EntityFormState(kind,entityName,"false",Some(id),public)
    def entity(query:Option[JSONQuery]) = EntityTableState(kind,entityName,query.map(js => window.btoa(js.asJson.noSpaces)),public)
    def entity(name:String) = EntityTableState(kind,name,None,public)
  }

  def getUrl(fa:FormAction, data: Json, kind: String, name: String, id: Option[String], writable: Boolean): Option[String] = fa.afterActionGoTo.map { x =>
    val urlInternalSubstitutions = x.replace("$kind", kind)
      .replace("$name", name)
      .replace("$id", id.getOrElse(""))
      .replace("$writable", writable.toString)

    MustacheUtils.render(urlInternalSubstitutions,data)


  }

}

