package ch.wsl.box.client.utils

import ch.wsl.box.model.shared.JSONID

object TestHooks {
  def langSwitch(lang:String) = s"langSwitch_$lang"
  def tableChildId(id:Int) = s"tableChildFormId$id"
  val tableChildRow = s"tableChildRow"
  def addChildId(id:Int) = s"addChildFormId$id"
  def deleteChildId(formId:Int,rowId:Option[JSONID]) = s"deleteChildFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def tableChildButtonId(formId:Int,rowId:Option[JSONID]) = s"tableChildButtonFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def tableChildRowId(formId:Int,rowId:Option[JSONID]) = s"tableChildRowFormId${formId}Row${rowId.map(_.asString).getOrElse("noid")}"
  def actionButton(label:String) = s"formAction${label.replace(" ","").toLowerCase}"
  val logoutButton = "logoutButton"
  val dataChanged = "dataChanged"
  def formField(name:String) = s"formField$name"
  def readOnlyField(name:String) = s"readOnlyField$name"

  private val loadingListeners = new scala.collection.mutable.ListBuffer[() => Unit]

  def loaded() = {
    loadingListeners.foreach(f => f())
  }

  def addOnLoad(f:() => Unit) = {
    loadingListeners.addOne(f)
  }
}
