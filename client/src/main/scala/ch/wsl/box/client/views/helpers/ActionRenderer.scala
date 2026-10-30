package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels, Navigate, Notification, Record}
import ch.wsl.box.client.utils.{ListenerManager, TestHooks}
import ch.wsl.box.client.views.components.ModalStack
import ch.wsl.box.model.shared.{BackAction, CopyAction, Danger, DeleteAction, FormAction, JSONID, JSONMetadata, NewWindow, NoAction, Popup, Primary, RevertAction, SaveAction, SaveAndClosePopup, SaveLocalAction, Self, Std}
import io.circe.Json
import io.udash.properties.single.ReadableProperty
import io.udash._
import org.scalajs.dom.{Event, Node, window}
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import scalacss.StyleA
import scribe.Logging

import scala.concurrent.Future

case class ActionIntent(insert:Boolean,updateRight:Boolean,write:Boolean)

class ActionRenderer(
                      handler: => ActionHandler,
                      listenerManager: ListenerManager,
                      metadata:JSONMetadata,
                      intent:ActionIntent,
                      reset:Unit => Unit

                    ) extends Logging {

  import ch.wsl.box.client.Context._
  import Implicits._
  import listenerManager._

  def render(_id:Option[String])(action:FormAction):Seq[Node] = {

    val importance:StyleA = action.importance match {
      case Primary => ClientConf.style.boxButtonImportant
      case Danger => ClientConf.style.boxButtonDanger
      case Std => ClientConf.style.boxButton
    }

    logger.debug(s"Render action $action for ${_id} with intent $intent")

    if(
      (!action.needUpdateRight || intent.updateRight) &&
        ((action.updateOnly && _id.isDefined && intent.updateRight) || (action.insertOnly && intent.insert) || (!action.insertOnly && !action.updateOnly))
    ) {
      val actionButton = button(
        id := TestHooks.actionButton(action.label),
        importance
      )(Labels(action.label)).render.listen(listenerManager.Event.click,e => actionClick(_id,action)(e))
      action.condition match {
        case Some(conditions) => {
          div(showIf(handler.data.transform{ js => conditions.check(js) })( actionButton )).render
        }
        case None => actionButton
      }
    } else Seq[Node]()

  }



  def actionClick(_id:Option[String],action:FormAction):Event => Any  = {

    logger.debug(s"Action $action clicked")

    def executeFunction():Future[Option[Boolean]] = action.executeFunction match {
      case Some(value) => services.rest.execute(value,services.clientSession.lang(),handler.data.get).map{ result =>
        result.errorMessage match {
          case Some(value) => {
            Notification.add(value)
            services.clientSession.loading.set(false)
            Some(false)
          }
          case None => Some(true)
        }
      }
      case None => Future.successful(None)
    }

    def afterSaveAction(combined:(JSONID,Record)):Future[Unit] = {

      val (id,record) = combined

      handler.afterSave(id,record)

      def onSuccess = Routes.getUrl(action, handler.data.get, metadata.kind, metadata.name, Some(id.asString), intent.write) match {
        case Some(url) => {
          logger.warn(s"Navigating to $url")
          //reset()
          Navigate.toUrl(url,action.target)
        }
        case None => ()
      }

      executeFunction().map {
        case Some(true) => {
          if (action.reload) {
            handler.reload(id)
          }
          onSuccess
        }
        case None => onSuccess
        case Some(false) => ()

      }
    }

    def callBack() = action.action match {
      case SaveAction =>  handler.beforeSave(action.html5check).flatMap(handler.saveToDb).map(afterSaveAction)
      case SaveLocalAction =>  handler.beforeSave(action.html5check).flatMap(handler.saveLocally).map(afterSaveAction)
      case SaveAndClosePopup =>  handler.beforeSave(action.html5check).flatMap(handler.saveToDb).map{ _ => ModalStack.mainStack.removeLast() }
      case NoAction => Routes.getUrl(action,handler.data.get,metadata.kind,metadata.name,_id,intent.write).foreach{ url =>
        executeFunction().flatMap {
          case Some(true) => {
            if (Navigate.canGoAway)
              reset()
            Navigate.toUrl(url,action.target)
          }
          case None => Navigate.toUrl(url,action.target)
          case Some(false) => Future.successful()

        }.foreach{ _ =>
          if(action.reload) handler.reload()
          else services.clientSession.loading.set(false)
        }
      }
      case CopyAction => handler.duplicate()
      case DeleteAction => handler.delete(_id.flatMap(JSONID.fromString(_,metadata)),handler.data.get,intent.write,action)
      case RevertAction => handler.reload()
      case BackAction => Navigate.back()

    }

    def confirm(cb: () => Any) =  action.confirmText match {
      case Some(ct) => {
        val confim = window.confirm(Labels(ct))
        if(confim) {
          cb()
        }
      }
      case None => cb()
    }



    (ev: Event) => {
      logger.info(s"Execution action $action")
      handler.beforeAction()
      confirm(callBack)
      ev.preventDefault()
    }
  }


}
