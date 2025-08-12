package ch.wsl.box.model.shared

trait LabelsCollection {
  def all:Seq[String]
}

object SharedLabels extends LabelsCollection {

  object messages extends LabelsCollection {
    def confirm = "messages.confirm"
    def all = Seq(confirm)
  }

  object subform extends LabelsCollection {
    def remove = "subform.remove"
    def add = "subform.add"
    def all = Seq(
      remove,
      add
    )
  }

  object error extends LabelsCollection{
    def notfound = "error.notfound"
    def session_expired = "error.session_expired"
    def all = Seq(
      notfound,
      session_expired
    )
  }

  object login extends LabelsCollection{
    def failed = "login.failed"
    def title = "login.title"
    def username = "login.username"
    def password = "login.password"
    def button = "login.button"
    def choseLang = "login.chose_lang"
    def all = Seq(
      failed,
      title,
      username,
      password,
      button,
      choseLang
    )
  }

  object table extends LabelsCollection {
    def showMore = "table.showMore"
    def showLess = "table.showLess"
    def all = Seq(
      showMore,
      showLess
    )
  }

  object navigation extends LabelsCollection{
    def recordFound = "navigation.recordFound"
    def recordsFiltered = "navigation.recordsFiltered"
    def recordsSelected = "navigation.recordsSelected"
    def removeSelection = "navigation.removeSelection"
    def selectAll = "navigation.selectAll"
    def goAway = "navigation.goAway"
    def first = "navigation.first"
    def last = "navigation.last"
    def next = "navigation.next"
    def previous = "navigation.previous"
    def firstPage = "navigation.first"
    def lastPage = "navigation.last"
    def nextPage = "navigation.next"
    def previousPage = "navigation.previous"
    def loading = "navigation.loading"
    def page = "navigation.page"
    def record = "navigation.record"
    def of = "navigation.of"
    def all = Seq(
      recordFound,
      goAway,
      first,
      last,
      next,
      previous,
      firstPage,
      lastPage,
      nextPage,
      previousPage,
      loading,
      page,
      record,
      of,
      recordsSelected,
      selectAll,
      removeSelection
    )
  }

  object filter  extends LabelsCollection{
    def not = "table.filter.not"
    def equals = "table.filter.equals"
    def contains = "table.filter.contains"
    def without = "table.filter.without"
    def between = "table.filter.between"
    def lt = "table.filter.lt"
    def gt = "table.filter.gt"
    def lte = "table.filter.lte"
    def gte = "table.filter.gte"
    def in = "table.filter.in"
    def none = "table.filter.none"
    def notin = "table.filter.notin"
    def all = Seq(
      not,
      equals,
      contains,
      without,
      between,
      lt,
      gt,
      lte,
      gte,
      in,
      none,
      notin
    )
  }

  object sort extends LabelsCollection{
    def asc = "sort.asc"
    def desc = "sort.desc"
    def all = Seq(
      asc,desc
    )
  }

  object form extends LabelsCollection{
    def required = "form.required"
    def save = "form.save"
    def save_local = "form.save_local"
    def save_add = "form.save_add"
    def save_table = "form.save_table"
    def addDate = "form.add_date"
    def removeDate = "form.remove_date"
    def changed = "form.changed"
    def removeMap = "form.remove-map"
    def removeFile = "form.remove-image"
    def drop = "form.drop"
    def print = "form.print"
    def trueLabel = "form.trueLabel"
    def falseLabel = "form.falseLabel"
    def all = Seq(
      required,
      save,
      save_local,
      save_add,
      save_table,
      addDate,
      removeDate,
      changed,
      removeMap,
      removeFile,
      drop,
      print,
      trueLabel,
      falseLabel
    )
  }

  object lookup extends LabelsCollection{
    def not_found = "lookup.not_found"
    def all = Seq(
      not_found
    )
  }

  object entities extends LabelsCollection{
    def search = "entity.search"
    def title = "entity.title"
    def select = "entity.select"
    def `new` = "entity.new"
    def table = "entity.table"
    def duplicate = "entity.duplicate"
    def all = Seq(
      search,
      title,
      select,
      `new`,
      table,
      duplicate
    )
  }

  object exports extends LabelsCollection{
    def search = "exports.search"
    def title = "exports.title"
    def select = "exports.select"
    def load = "exports.load"
    def csv = "exports.csv"
    def xls = "exports.xls"
    def pdf = "exports.pdf"
    def html = "exports.html"
    def shp = "exports.shp"
    def all = Seq(
      search,
      title,
      select,
      load,
      csv,
      xls,
      pdf,
      html,
      shp
    )
  }

  object entity extends LabelsCollection{
    def filters = "table.filters"
    def actions = "table.actions"
    def show = "table.show"
    def edit = "table.edit"
    def no_action = "table.no_action"
    def delete = "table.delete"
    def revert = "table.revert"
    def confirmDelete = "table.confirmDelete"
    def confirmRevert = "table.confirmRevert"
    def csv = "table.csv"
    def xls = "table.xls"
    def shp = "table.shp"
    def geopackage = "table.geopackage"
    def all = Seq(
      filters,
      actions,
      show,
      edit,
      no_action,
      delete,
      revert,
      confirmDelete,
      confirmRevert,
      csv,
      xls,
      shp,
      geopackage
    )
  }

  object header extends LabelsCollection{
    def home = "header.home"
    def entities = "header.entities"
    def tables = "header.tables"
    def views = "header.views"
    def forms = "header.forms"
    def exports = "header.exports"
    def functions = "header.functions"
    def lang = "header.lang"
    def all = Seq(
      home,
      entities,
      tables,
      views,
      forms,
      exports,
      functions,
      lang
    )
  }

  object popup extends LabelsCollection{
    def search = "popup.search"
    def close = "popup.close"
    def remove = "popup.remove"
    def all = Seq(
      search,
      close,
      remove
    )
  }

  object home extends LabelsCollection {
    def title = "ui.index.title"
    def all = Seq(
      title
    )
  }

  object map extends LabelsCollection {
    def panZoom = "ui.map.panZoom"
    def edit = "ui.map.edit"
    def addPoint = "ui.map.addPoint"
    def addPointGPS = "ui.map.addPointGPS"
    def addLine = "ui.map.addLine"
    def addPolygon = "ui.map.addPolygon"
    def addPolygonHole = "ui.map.addPolygonHole"
    def move = "ui.map.move"
    def delete = "ui.map.delete"
    def goTo = "ui.map.goTo"
    def goToGPS = "ui.map.goToGPS"
    def insertPoint = "ui.map.insertPoint"
    def insertPointGPS = "ui.map.insertPointGPS"
    def drawOnMap = "ui.map.drawOnMap"
    def drawOrEnter = "ui.map.drawOrEnter"

    override def all: Seq[String] = Seq(panZoom,edit,addPoint,addLine,addPolygon,addPolygonHole,move,delete,goTo,insertPoint,goToGPS,insertPointGPS,addPointGPS,drawOnMap,drawOrEnter)
  }

  def all = Seq(messages,subform,error,login,navigation,filter,sort,form,lookup,entities,exports,entity,header,popup,home,map).flatMap(_.all)


}
