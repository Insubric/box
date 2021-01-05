package ch.wsl.box.client.services

import ch.wsl.box.client.{Context, IndexState, LoginState}
import ch.wsl.box.model.shared.{IDs, JSONID, JSONQuery, LoginRequest}
import io.udash.properties.single.Property
import io.udash.routing.RoutingRegistry
import org.scalajs.dom
import scribe.Logging

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by andre on 5/24/2017.
  */

case class SessionQuery(query:JSONQuery, entity:String)
object SessionQuery{
  def empty = SessionQuery(JSONQuery.empty,"")
}

object ClientSession {
  final val QUERY = "query"
  final val IDS = "ids"
  final val USER = "user"
  final val LANG = "lang"
  final val LABELS = "labels"
  final val TABLECHILD_OPEN = "tablechild_open"

  case class TableChildElement(field:String,childFormId:Int,id:Option[JSONID])
}

class ClientSession(rest:REST,httpClient: HttpClient) extends Logging {

  import Context._
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._
  import ClientSession._


  final private val BASE_LAYER = "base_layer"

  lazy val logged = {
    Property(false)
  }

  logger.info("Loading session")


  isValidSession().map{ x =>
    logger.info(s"is valid session $x")
    x match {
      case true => {
        logger.info("Valid session found")
        logged.set(true)
      }
      case false => {
        logger.info("No valid session found")
        if(isSet(ClientSession.USER)) {
          dom.window.sessionStorage.removeItem(USER)
          dom.window.location.reload(true)
        }
      }
    }
  }

  httpClient.setHandleAuthFailure(() => {
    logger.info("Authentication failure, trying to get a new valid session")
    LoginPopup.show()
  })

  def set[T](key:String,obj:T)(implicit encoder: Encoder[T]) = {
    logger.info(s"Setting $key")
    dom.window.sessionStorage.setItem(key,obj.asJson.toString())
  }

  def get[T](key:String)(implicit decoder: Decoder[T]):Option[T] = {
    val raw = dom.window.sessionStorage.getItem(key)
    for{
      json <- parse(raw).right.toOption
      query <- json.as[T].right.toOption
    } yield query
  }

  def isValidSession():Future[Boolean] = {
    isSet(USER) match {
      case false => Future.successful(false)
      case true => rest.validSession()
    }
  }

  def isSet(key:String):Boolean = {
    Try(dom.window.sessionStorage.getItem(key).size > 0).isSuccess
  }

  def login(username:String,password:String):Future[Boolean] = {
    createSession(username,password).map{ _ =>
      Context.applicationInstance.reload()
      true
    }
  }

  def createSession(username:String,password:String):Future[Boolean] = {
    dom.window.sessionStorage.setItem(USER,username)
    val fut = for{
      _ <- rest.login(LoginRequest(username,password))
      ui <- rest.ui()
    } yield {
      UI.load(ui)
      logged.set(true)
      true
    }

    fut.recover{ case t =>
      dom.window.sessionStorage.removeItem(USER)
      logged.set(false)
      t.printStackTrace()
      false
    }
  }



  def logout() = {
    Navigate.toAction{ () =>
      dom.window.sessionStorage.removeItem(USER)
      for{
        _ <- rest.logout()
        ui <- rest.ui()
      } yield {
        UI.load(ui)
        logged.set(false)
        Navigate.to(LoginState(""))
      }
    }
  }

  def getQuery():Option[SessionQuery] = get[SessionQuery](QUERY)
  def setQuery(query: SessionQuery) = set(QUERY,query)
  def resetQuery() = set(QUERY, None)

  def getBaseLayer():Option[String] = get[String](BASE_LAYER)
  def setBaseLayer(bl: String) = set(BASE_LAYER,bl)


  def getIDs():Option[IDs] = get[IDs](IDS)
  def setIDs(ids:IDs) = set(IDS, ids)
  def resetIDs() = set(IDS, None)



  def isTableChildOpen(tc:TableChildElement):Boolean = get[Seq[TableChildElement]](TABLECHILD_OPEN).toSeq.flatten.contains(tc)
  def setTableChildOpen(tc:TableChildElement) = set(
    TABLECHILD_OPEN,
    (get[Seq[TableChildElement]](TABLECHILD_OPEN).toSeq.flatten ++ Seq(tc)).distinct
  )
  def setTableChildClose(tc:TableChildElement) = set(
    TABLECHILD_OPEN,
    get[Seq[TableChildElement]](TABLECHILD_OPEN).toSeq.flatten.filterNot(_ == tc)
  )

  def lang():String = {

    val sessionLang = Try(dom.window.sessionStorage.getItem(LANG)).toOption
    val browserLang = dom.window.navigator.language

    (sessionLang,browserLang) match {
      case (Some(lang),_) if ClientConf.langs.contains(lang)  => lang
      case (_,lang) if ClientConf.langs.contains(lang)  => lang
      case _ if ClientConf.langs.nonEmpty => ClientConf.langs.head
      case _ => "en"
    }
  }
  def setLang(lang:String) = rest.labels(lang).map{ labels =>
    Labels.load(labels)
    dom.window.sessionStorage.setItem(LANG,lang)
    Context.applicationInstance.reload()
  }



}
