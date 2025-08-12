package ch.wsl.box.client.services


import scala.util.Try

object UI {

  import io.circe._
  import io.circe.generic.auto._


  private var ui:Map[String,String] = Map()

  def load(ui:Map[String,String]) = {
    this.ui = ui
  }

  case class MenuEntry(name:String,url:String)

  def logo = ui.lift("logo")
  def title = ui.lift("title")
  def indexTitle = Labels.home.title(ui.get("index.title"))
  def loginTopHtml = Labels(ui.lift("login.html.top").getOrElse(""))
  def loginBottomHtml = Labels(ui.lift("login.html.bottom").getOrElse(""))
  def indexHtml = Labels(ui.lift("index.html").getOrElse(""))
  def indexPage = ui.lift("index.page").filterNot(_.trim == "")
  def footerCopyright = ui.lift("footerCopyright")
  def debug = ui.lift("debug").contains("true")
  def enableAllTables = ui.lift("enableAllTables").contains("true")
  def showEntitiesSidebar = ui.lift("showEntitiesSidebar").contains("true")
  def menu = ui.lift("menu").toSeq.flatMap{ m =>
    Try {
      parser.parse(m).right.get.as[Seq[MenuEntry]].right.get
    }.toOption
  }.flatten

  def enabledFilters(filters:Seq[String]):Seq[String] = {

    Try(ui("filters.enable")).toOption match {
      case Some("all") => filters
      case Some(value) => value.split(",").toSeq.intersect(filters)
      case None => filters
    }

  }


}
