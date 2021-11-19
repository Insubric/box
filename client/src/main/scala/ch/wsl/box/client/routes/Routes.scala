package ch.wsl.box.client.routes

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.{EntityFormState, EntityTableState, RoutingState}
import ch.wsl.box.model.shared.JSONQuery
import org.scalajs.dom
import org.scalajs.dom.experimental.URLSearchParams
import scribe.Logging

import scala.scalajs.js

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

  def apply(kind:String, entityName:String) = new Routes{
    def add() = EntityFormState(kind,entityName,"true",None,false)
    def edit(id:String) = EntityFormState(kind,entityName,"true",Some(id),false)
    def show(id:String) = EntityFormState(kind,entityName,"false",Some(id),false)
    def entity(query:Option[JSONQuery]) = EntityTableState(kind,entityName,query.map(_.asJson.noSpaces))
    def entity(name:String) = EntityTableState(kind,name,None)
  }

}

