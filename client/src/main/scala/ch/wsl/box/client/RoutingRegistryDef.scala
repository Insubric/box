package ch.wsl.box.client

import ch.wsl.box.client.services.{ClientSession, Labels, Notification}
import io.udash._
import scribe.Logging

class RoutingRegistryDef extends RoutingRegistry[RoutingState] with Logging {
  import Context._
  def matchUrl(url: Url): RoutingState = {
    logger.info(s"match URL $url logged: ${services.clientSession.isSet(ClientSession.USER)}")
    services.clientSession.isSet(ClientSession.USER) match {
      //case true => loggedInUrl2State.applyOrElse (url.value.stripSuffix ("/"), (x: String) => ErrorState)
      case true => loggedInUrl2State.applyOrElse (url.value.stripSuffix ("/"), (x: String) => {
        Notification.add(Labels.error.notfound + " " + url)
        IndexState
      })
      case false => loggedOutUrl2State.applyOrElse (url.value.stripSuffix ("/"), (x: String) => {
        logger.info(s"here $url")
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
    case "/entities" => EntitiesState("entity","")
    case "/tables" => EntitiesState("table","")
    case "/views" => EntitiesState("view","")
    case "/forms" => EntitiesState("form","")
    case "/functions"  => DataListState(DataKind.FUNCTION,"")
    case "/exports"  => DataListState(DataKind.EXPORT,"")
    case "/box" / "export" / exportFunction  => DataState(DataKind.EXPORT,exportFunction)
    case "/box" / "function" / exportFunction  => DataState(DataKind.FUNCTION,exportFunction)
    case "/box" / kind / entity / "insert" => EntityFormState(kind,entity,"true",None,false)
    case "/box" / kind / entity / "row" / write / id  => EntityFormState(kind,entity,write,Some(id),false)
    case "/box" / kind / entity / "child" / childEntity => MasterChildState(kind,entity,childEntity)
    case "/box" / kind / entity => EntityTableState(kind,entity)
    case "/admin"  => AdminState
  }

  private val (loggedOutUrl2State, loggedOutState2Url) = bidirectional {
    case "" => LoginState("")
    case "/logout" => LogoutState
    case "/public" / "box" / kind / entity / "insert" => EntityFormState(kind,entity,"true",None,true)
    case "/entities" => LoginState("/entities")
    case "/tables" => LoginState("/tables")
    case "/views" => LoginState("/views" )
    case "/forms" => LoginState("/forms")
    case "/functions"  => LoginState("/functions")
    case "/exports"  => LoginState("/exports")
    case "/box" / "export" / exportFunction  => LoginState1param("/box/export/",exportFunction)
    case "/box" / "" / exportFunction  => LoginState1param("/box/function/",exportFunction)
    case "/box" / kind / entity / "insert" => LoginState2params("/box/kind/entity/insert",kind,entity)
    case "/box" / kind / entity / "row" / write / id  => LoginState4params("/box/kind/entity/row/write/id",kind,entity,write,id)
    case "/box" / kind / entity / "child" / childEntity => LoginState3params("/box/$kind/$entity/child/$childEntity",kind,entity,childEntity)
    case "/box" / kind / entity => LoginState2params("/box/$kind/$entity",kind,entity)
    case "/admin"  => LoginState("/admin")
  }
}