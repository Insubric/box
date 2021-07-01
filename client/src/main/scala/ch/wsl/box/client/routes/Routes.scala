package ch.wsl.box.client.routes

import ch.wsl.box.client.{EntityFormState, EntityTableState, RoutingState}
import ch.wsl.box.model.shared.JSONQuery
import org.scalajs.dom

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

object Routes {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._

  def apiV1(path:String = ""):String = {
    dom.window.location.port == "12345" match {
      case false =>  dom.window.location.pathname + "api/v1"+path
      case true => "http://localhost:8080/api/v1" + path
    }
  }

  def wsV1(topic:String):String = {
    dom.window.location.origin.getOrElse("").replace("http","ws") + "/api/v1/notifications/"+topic
  }

  def apply(kind:String, entityName:String) = new Routes{
    def add() = EntityFormState(kind,entityName,"true",None,false)
    def edit(id:String) = EntityFormState(kind,entityName,"true",Some(id),false)
    def show(id:String) = EntityFormState(kind,entityName,"false",Some(id),false)
    def entity(query:Option[JSONQuery]) = EntityTableState(kind,entityName,query.map(_.asJson.noSpaces))
    def entity(name:String) = EntityTableState(kind,name,None)
  }

}

