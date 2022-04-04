package ch.wsl.box.client

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientSession, Labels, Notification}
import io.udash._
import org.scalajs.dom
import scribe.Logging

class RoutingRegistryDef extends RoutingRegistry[RoutingState] with Logging {
  import Context._
  def matchUrl(url: Url): RoutingState = {
    val localUrl = if(dom.window.location.hash.startsWith("#/")) dom.window.location.hash.stripPrefix("#") else url.value
    logger.info(s"match URL $localUrl logged: ${services.clientSession.isSet(ClientSession.USER)}")
    services.clientSession.isSet(ClientSession.USER) match {
      //case true => loggedInUrl2State.applyOrElse (url.value.stripSuffix ("/"), (x: String) => ErrorState)
      case true => loggedInUrl2State.applyOrElse ( localUrl.stripSuffix ("/"), (x: String) => {
        Notification.add(Labels.error.notfound + " " + localUrl)
        IndexState
      })
      case false => loggedOutUrl2State.applyOrElse (localUrl.stripSuffix ("/"), (x: String) => {
        logger.info(s"here $localUrl")
        ErrorState
      })
    }
  }

  def matchState(state: RoutingState): Url = {
    logger.info(s"match STATE ${services.clientSession.isSet(ClientSession.USER)}")
    services.clientSession.isSet(ClientSession.USER) match {
      case true => Url(loggedInState2Url.apply(state))
      case false => Url(loggedOutState2Url.apply(state))
    }
  }


  private val (loggedInUrl2State, loggedInState2Url) = bidirectional {
    case "" => IndexState
    case "/entities" => EntitiesState("entity","",Layouts.std)
    case "/tables" => EntitiesState("table","",Layouts.std)
    case "/views" => EntitiesState("view","",Layouts.std)
    case "/forms" => EntitiesState("form","",Layouts.std)
    case "/functions"  => DataListState(DataKind.FUNCTION,"")
    case "/exports"  => DataListState(DataKind.EXPORT,"")
    case "/box" / "export" / exportFunction  => DataState(DataKind.EXPORT,exportFunction)
    case "/box" / "function" / exportFunction  => DataState(DataKind.FUNCTION,exportFunction)
    case "/box" / kind / entity / "page" => FormPageState(kind,entity,"true",false,Layouts.std)
    case "/box" / kind / entity / "insert" => EntityFormState(kind,entity,"true",None,false,Layouts.std)
    case "/box" / kind / entity / "row" / write / id  => EntityFormState(kind,entity,write,Some(id),false,Layouts.std)
    case "/box" / kind / entity / "child" / childEntity => MasterChildState(kind,entity,childEntity)
    case "/box" / kind / entity => EntityTableState(kind,entity,None)
    case "/box" / kind / entity / "query" / query => EntityTableState(kind,entity,Some(query))
    case "/translations"  => TranslatorState
    case "/admin"  => AdminState
    case "/admin" / "box-definition"  => AdminBoxDefinitionState
    case "/admin" / "translations" / from / to  => AdminTranslationsState(from,to)
    case "/admin" / "conf"  => AdminConfState
    case "/admin" / "ui-conf"  => AdminUiConfState
  }

  private val (loggedOutUrl2State, loggedOutState2Url) = bidirectional {
    case "" => LoginState("")
    case "/logout" => LogoutState
    case "/public" / "box" / kind / entity / "page" => FormPageState(kind,entity,"true",false,Layouts.std)
    case "/public" / "box" / kind / entity / "insert" / "blank" => EntityFormState(kind,entity,"true",None,true,Layouts.blank)
    case "/public" / "box" / kind / entity / "insert"  => EntityFormState(kind,entity,"true",None,true,Layouts.std)
    case "/public" / "box" / kind / entity / "row" / write / id  => EntityFormState(kind,entity,write,Some(id),true,Layouts.std)
    case "/public" / "box" / kind / entity / "row" / write / id / "blank" => EntityFormState(kind,entity,write,Some(id),true,Layouts.blank)
    case "/entities" => LoginState("/entities")
    case "/tables" => LoginState("/tables")
    case "/views" => LoginState("/views" )
    case "/forms" => LoginState("/forms")
    case "/functions"  => LoginState("/functions")
    case "/exports"  => LoginState("/exports")
    case "/box" / "export" / exportFunction  => LoginState1param("/box/export/",exportFunction)
    case "/box" / "" / exportFunction  => LoginState1param("/box/function/",exportFunction)
    case "/box" / kind / entity / "page" => LoginState2params("/box/kind/entity/page",kind,entity)
    case "/box" / kind / entity / "insert" => LoginState2params("/box/kind/entity/insert",kind,entity)
    case "/box" / kind / entity / "row" / write / id  => LoginState4params("/box/kind/entity/row/write/id",kind,entity,write,id)
    case "/box" / kind / entity / "child" / childEntity => LoginState3params("/box/$kind/$entity/child/$childEntity",kind,entity,childEntity)
    case "/box" / kind / entity => LoginState2params("/box/$kind/$entity",kind,entity)
    case "/box" / kind / entity / "query" / query => LoginState3params("/box/$kind/$entity/query/$query",kind,entity,query)
    case "/translations"  => LoginState("/translations")
    case "/admin"  => LoginState("/admin")
    case "/admin" / "box-definition"  => LoginState("/admin/box-definition")
    case "/admin" / "translations" / from / to  => LoginState2params("/admin/translations/$from/$to",from,to)
    case "/admin" / "conf"  => LoginState("/admin/conf")
    case "/admin" / "ui-conf"  => LoginState("/admin/ui-conf")
  }
}