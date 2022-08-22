package ch.wsl.box.client.services

import ch.wsl.box.model.shared.SharedLabels

/**
  * Created by andre on 6/8/2017.
  */
object Labels {

//  def langs = Seq("it","de","fr","en)

  private var labels:Map[String,String] = Map()

  def load(table:Map[String,String]) = {
    labels = table
  }

  def apply(key:String):String = get(key)
  def all:Map[String,String] = labels

  private def get(key:String):String = labels.lift(key).filterNot(_.trim == "").getOrElse(key)
  private def getOpt(key:String):Option[String] = labels.lift(key).filterNot(_.trim == "")

  object messages {
    def confirm = get(SharedLabels.messages.confirm)
  }

  object subform {
    def remove = get(SharedLabels.subform.remove)
    def add = get(SharedLabels.subform.add)
  }

  object error{
    def notfound = get(SharedLabels.error.notfound)
    def session_expired = get(SharedLabels.error.session_expired)
  }

  object login{
    def failed = get(SharedLabels.login.failed)
    def title = get(SharedLabels.login.title)
    def username = get(SharedLabels.login.username)
    def password = get(SharedLabels.login.password)
    def button = get(SharedLabels.login.button)
    def choseLang = get(SharedLabels.login.choseLang)
  }

  object navigation{
    def recordFound = get(SharedLabels.navigation.recordFound)
    def goAway = get(SharedLabels.navigation.goAway)
    def first = get(SharedLabels.navigation.first)
    def last = get(SharedLabels.navigation.last)
    def next = get(SharedLabels.navigation.next)
    def previous = get(SharedLabels.navigation.previous)
    def firstPage = get(SharedLabels.navigation.first)
    def lastPage = get(SharedLabels.navigation.last)
    def nextPage = get(SharedLabels.navigation.next)
    def previousPage = get(SharedLabels.navigation.previous)
    def loading = get(SharedLabels.navigation.loading)
    def page = get(SharedLabels.navigation.page)
    def record = get(SharedLabels.navigation.record)
    def of = get(SharedLabels.navigation.of)
  }

  object filter {
    def not = get(SharedLabels.filter.not)
    def equals = get(SharedLabels.filter.equals)
    def contains = get(SharedLabels.filter.contains)
    def without = get(SharedLabels.filter.without)
    def between = get(SharedLabels.filter.between)
    def lt = get(SharedLabels.filter.lt)
    def gt = get(SharedLabels.filter.gt)
    def lte = get(SharedLabels.filter.lte)
    def gte = get(SharedLabels.filter.gte)
    def in = get(SharedLabels.filter.in)
    def none = get(SharedLabels.filter.none)
    def notin = get(SharedLabels.filter.notin)
  }

  object sort{
    def asc = get(SharedLabels.sort.asc)
    def desc = get(SharedLabels.sort.desc)
  }

  object form{
    def required = get(SharedLabels.form.required)
    def save = get(SharedLabels.form.save)
    def save_add = get(SharedLabels.form.save_add)
    def save_table = get(SharedLabels.form.save_table)
    def addDate = get(SharedLabels.form.addDate)
    def removeDate = get(SharedLabels.form.removeDate)
    def changed = get(SharedLabels.form.changed)
    def removeMap = get(SharedLabels.form.removeMap)
    def removeFile = get(SharedLabels.form.removeFile)
    def drop = get(SharedLabels.form.drop)
    def print = get(SharedLabels.form.print)
    def trueLabel = getOpt(SharedLabels.form.trueLabel)
    def falseLabel = getOpt(SharedLabels.form.falseLabel)
  }

  object lookup{
    def not_found = get(SharedLabels.lookup.not_found)
  }

  object entities{
    def search = get(SharedLabels.entities.search)
    def title = get(SharedLabels.entities.title)
    def select = get(SharedLabels.entities.select)
    def `new` = get(SharedLabels.entities.`new`)
    def table = get(SharedLabels.entities.table)
    def duplicate = get(SharedLabels.entities.duplicate)
  }

  object exports{
    def search = get(SharedLabels.exports.search)
    def title = get(SharedLabels.exports.title)
    def select = get(SharedLabels.exports.select)
    def load = get(SharedLabels.exports.load)
    def csv = get(SharedLabels.exports.csv)
    def pdf = get(SharedLabels.exports.pdf)
    def html = get(SharedLabels.exports.html)
    def shp = get(SharedLabels.exports.shp)
  }

  object table{
    def showMore = get(SharedLabels.table.showMore)
    def showLess = get(SharedLabels.table.showLess)
  }

  object entity{
    def filters = get(SharedLabels.entity.filters)
    def actions = get(SharedLabels.entity.actions)
    def show = get(SharedLabels.entity.show)
    def edit = get(SharedLabels.entity.edit)
    def no_action = get(SharedLabels.entity.no_action)
    def delete = get(SharedLabels.entity.delete)
    def revert = get(SharedLabels.entity.revert)
    def confirmDelete = get(SharedLabels.entity.confirmDelete)
    def confirmRevert = get(SharedLabels.entity.confirmRevert)
    def csv = get(SharedLabels.entity.csv)
    def xls = get(SharedLabels.entity.xls)
    def shp = get(SharedLabels.entity.shp)
  }

  object header{
    def home = get(SharedLabels.header.home)
    def entities = get(SharedLabels.header.entities)
    def tables = get(SharedLabels.header.tables)
    def views = get(SharedLabels.header.views)
    def forms = get(SharedLabels.header.forms)
    def exports = get(SharedLabels.header.exports)
    def functions = get(SharedLabels.header.functions)
    def lang = get(SharedLabels.header.lang)
  }

  object popup{
    def search = get(SharedLabels.popup.search)
    def close = get(SharedLabels.popup.close)
    def remove = get(SharedLabels.popup.remove)
  }

  object home {
    def title(uiTitle:Option[String]) = uiTitle.getOrElse(get(SharedLabels.home.title))
  }

  object map {
    def panZoom = get(SharedLabels.map.panZoom)
    def edit = get(SharedLabels.map.edit)
    def addPoint = get(SharedLabels.map.addPoint)
    def addLine = get(SharedLabels.map.addLine)
    def addPolygon = get(SharedLabels.map.addPolygon)
    def addPolygonHole = get(SharedLabels.map.addPolygonHole)
    def move = get(SharedLabels.map.move)
    def delete = get(SharedLabels.map.delete)
    def goTo = get(SharedLabels.map.goTo)
    def goToGPS = get(SharedLabels.map.goToGPS)
    def insertPoint = get(SharedLabels.map.insertPoint)
    def insertPointGPS = get(SharedLabels.map.insertPointGPS)
    def drawOnMap = get(SharedLabels.map.drawOnMap)
    def drawOrEnter = get(SharedLabels.map.drawOrEnter)
  }
}
