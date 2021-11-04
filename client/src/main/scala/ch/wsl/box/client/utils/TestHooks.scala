package ch.wsl.box.client.utils

import java.util.UUID
import ch.wsl.box.model.shared.JSONID
import io.circe.Json
import io.udash.properties.single.Property

import scala.concurrent.{ExecutionContext, Future, Promise}

object TestHooks {

  var testing = false

  def langSwitch(lang:String) = s"langSwitch_$lang"
  def tableChildId(id:UUID) = s"tableChildFormId$id"
  val tableChildRow = s"tableChildRow"
  def addChildId(id:UUID) = s"addChildFormId$id"
  def deleteChildId(formId:UUID,rowId:Option[JSONID]) = s"deleteChildFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def tableChildButtonId(formId:UUID,rowId:Option[JSONID]) = s"tableChildButtonFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def tableChildRowId(formId:UUID,rowId:Option[JSONID]) = s"tableChildRowFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def actionButton(label:String) = s"formAction${label.replace(" ","").toLowerCase}"
  val logoutButton = "logoutButton"
  val dataChanged = "dataChanged"
  def formField(name:String) = s"formField$name"
  def readOnlyField(name:String) = s"readOnlyField$name"

  private val loadingListeners = new scala.collection.mutable.ListBuffer[() => Unit]

  private var promises:Seq[Future[Boolean]] = Seq()

  def addLoadedPromise(p:Promise[Boolean])(implicit ec:ExecutionContext) = {
    val fut = p.future
    fut.foreach{ _ =>
      if(promises.forall(_.isCompleted)) loaded()
    }
    promises = promises ++ Seq(fut)
  }

  private def loaded() = {
    promises = Seq()
    loadingListeners.foreach(f => f())
  }

  def addOnLoad(f:() => Unit) = {
    loadingListeners.addOne(f)
  }

  var properties:scala.collection.mutable.Map[String,Property[Json]] = scala.collection.mutable.Map()

}
