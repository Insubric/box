package ch.wsl.box.client.services

import ch.wsl.box.client.routes.Routes

import java.util.UUID
import ch.wsl.box.client.{Context, IndexState, LoginState, LogoutState}
import ch.wsl.box.model.shared.oidc.UserInfo
import ch.wsl.box.model.shared.{CurrentUser, EntityKind, IDs, JSONID, JSONQuery, LoginRequest}
import io.udash.properties.single.Property
import io.udash.routing.RoutingRegistry
import org.scalajs.dom
import org.scalajs.dom.experimental.URLSearchParams
import scribe.Logging

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by andre on 5/24/2017.
  */



object ClientSession {
  final val QUERY = "query"
  final val TABS = "tabs"
  final val IDS = "ids"
  final val USER = "user"
  final val LANG = "lang"
  final val LABELS = "labels"
  final val TABLECHILD_OPEN = "tablechild_open"
  final val SELECTED_TAB = "selected_tab"
  final val URL_QUERY = "urlQuery"

  case class TableChildElement(field:String, childFormId:UUID, id:Option[JSONID])
  case class SelectedTabKey(form:UUID, tabGroup:Option[String])
  case class SelectedTabElement(key:SelectedTabKey, selected:String)
}

class ClientSession(rest:REST,httpClient: HttpClient) extends Logging {

  import Context._
  import Context.Implicits._
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._
  import ClientSession._


  final private val BASE_LAYER = "base_layer"

  lazy val logged = {
    Property(false)
  }

  val loading = Property(false)
  private var userInfo:Option[UserInfo] = None

  logger.info("Loading session")

  val parameters = new URLSearchParams(dom.window.location.search)
  Try(parameters.get("lang")).toOption.foreach { l =>
    if (l != null && l != "null" && l.nonEmpty ) {
      if (l.length > 1) {
        logger.info(s"Setting language session $l")
        dom.window.sessionStorage.setItem(LANG, l)
      }
      parameters.delete("lang")
      parameters.toString
      dom.window.location.href = dom.window.location.href.takeWhile(_ != '?') + {if(parameters.nonEmpty) "?" + parameters else ""}
    }
  }



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
          dom.window.location.reload()
        }
      }
    }
  }

  httpClient.setHandleAuthFailure(() => {
    if(logged.get) {
      logger.info("Authentication failure, trying to get a new valid session")
      services.clientSession.loading.set(false)
      LoginPopup.show()
    }
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

  def refreshSession():Future[Boolean] = {
    rest.me().flatMap(createSession).recover{case t:Throwable =>
      logger.warn(s"Session non valid")
      dom.window.sessionStorage.removeItem(USER)
      Notification.closeWebsocket()
      logged.set(false)
      false
    }
  }

  def isSet(key:String):Boolean = {
    val item = dom.window.sessionStorage.getItem(key)
    if(item == null) return false
    item.nonEmpty
  }

  def login(username:String,password:String):Future[Boolean] = {
    createSessionUserNamePassword(username,password).map{ valid =>
      logger.info(s"New session, valid: $valid")
      if(valid) {
        resetAllQueries()
        Context.applicationInstance.reload()
      }
      valid
    }
  }

  def createSessionUserNamePassword(username:String,password:String):Future[Boolean] = {

    rest.login(LoginRequest(username,password)).flatMap(createSession)

  }

  def createSession(user:UserInfo) = {

    rest.ui().map(UI.load).map{ _ =>
      dom.window.sessionStorage.setItem(USER,user.preferred_username)
      userInfo = Some(user)
      Notification.setUpWebsocket()
      logged.set(true)
      true
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

        Notification.closeWebsocket()

        val oldState = Context.applicationInstance.currentState
        Navigate.to(LoginState(""))

        logger.info(oldState.toString)

        if (oldState == IndexState) { // Fix #113
          logger.info("Reloading...")
          Context.applicationInstance.reload()
        }


      }
    }
  }

  private def queryKey(kind:String,form:String,urlQuery:JSONQuery):String = s"${new EntityKind(kind).entityOrForm}-$form-${urlQuery.hashCode()}"

  def getQueryFor(kind:String,form:String,urlQuery:Option[JSONQuery]):Option[JSONQuery] = {
    val key = queryKey(kind,form,urlQuery.getOrElse(JSONQuery.empty))
    logger.info(s"getQueryFor kind: $kind, form: $form -> $key")
    for {
      all <- get[Map[String,JSONQuery]](QUERY)
      q <- all.get(key)
    } yield {
      logger.info(s"found session Query: $q")
      q
    }
  }

  def setQueryFor(kind:String,form:String,urlQuery:Option[JSONQuery],query: JSONQuery):Unit = {
    val newQ = Map(queryKey(kind,form,urlQuery.getOrElse(JSONQuery.empty)) -> query)
    val queries:Map[String, JSONQuery] = get[Map[String, JSONQuery]](QUERY) match {
      case Some(value) => value ++ newQ
      case None => newQ
    }
    set(QUERY, queries)
  }

  def resetQuery(kind:String,form:String,urlQuery:Option[JSONQuery]):Unit = {
    val queries:Map[String, JSONQuery] = get[Map[String, JSONQuery]](QUERY) match {
      case Some(value) => value.view.filterKeys(_ != queryKey(kind,form,urlQuery.getOrElse(JSONQuery.empty))).toMap
      case None => Map()
    }
    set(QUERY, queries)
  }

  def resetAllQueries() = {
    dom.window.sessionStorage.removeItem(QUERY)
  }

  def getURLQuery():Option[JSONQuery] = get[JSONQuery](URL_QUERY)
  def setURLQuery(q: JSONQuery) = set(URL_QUERY,q)

  def getBaseLayer():Option[String] = get[String](BASE_LAYER)
  def setBaseLayer(bl: String) = set(BASE_LAYER,bl)


  def getIDs():Option[IDs] = get[IDs](IDS)
  def setIDs(ids:IDs) = set(IDS, ids)
  def resetIDs() = set(IDS, None)

  def selectedTab(key:SelectedTabKey):Option[String] = get[Seq[SelectedTabElement]](SELECTED_TAB).flatMap(_.find(_.key == key)).map(_.selected)
  def setSelectedTab(key:SelectedTabKey,tab:String):Unit = set(
    SELECTED_TAB,
    (get[Seq[SelectedTabElement]](TABLECHILD_OPEN).toSeq.flatten ++ Seq(SelectedTabElement(key,tab))).distinct
  )

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

    Routes.urlParams.get(LANG).foreach(l =>
      dom.window.sessionStorage.setItem(LANG,l)
    )
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

  def getRoles():Seq[String] = userInfo.map(_.roles).getOrElse(Seq())

  def getUserInfo():Option[UserInfo] = userInfo


  def isAdmin() = getRoles().contains("box_admin")


}
