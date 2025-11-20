package ch.wsl.box.client.views

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.db.{DB, LocalRecord}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.{Context, EntityFormState, EntityTableState, FormPageState}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels, Navigate, Navigation, Notification, UI}
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.{ElementId, TestHooks, URLQuery}
import ch.wsl.box.client.viewmodel.Row
import ch.wsl.box.client.views.components.ui.TwoPanelResize
import ch.wsl.box.client.views.components.widget.DateTimeWidget
import ch.wsl.box.client.views.components.{Debug, MapList, TableFieldsRenderer}
import ch.wsl.box.client.views.elements.Offline
import ch.wsl.box.client.views.helpers.TableColumnDrag
import ch.wsl.box.model.shared.EntityKind.VIEW
import ch.wsl.box.model.shared.GeoJson.Polygon
import ch.wsl.box.model.shared.geo.GeoDataRequest
import ch.wsl.box.model.shared.{JSONQuery, _}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.{BootstrapStyles, UdashBootstrap}
import io.udash.bootstrap.table.UdashTable
import io.udash.bootstrap.utils.UdashIcons
import io.udash.properties.single.Property
import io.udash.utils.Registration
import org.scalajs.dom
import org.scalajs.dom.html.{Div, TableCol}
import scalacss.ScalatagsCss._
import org.scalajs.dom.{Element, Event, HTMLElement, KeyboardEvent, MutationObserver, MutationObserverInit, document, window}
import scalacss.internal.Pseudo.Lang
import scalacss.internal.StyleA
import scalatags.JsDom.all.a
import scalatags.generic
import scribe.Logging
import ch.wsl.typings.choicesJs.anon.PartialOptions
import ch.wsl.typings.choicesJs.publicTypesSrcScriptsInterfacesInputChoiceMod.InputChoice
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.BackdropType
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.bootstrap.utils.BootstrapStyles.Size

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{URIUtils, |}
import scala.util.Try
import scala.scalajs.js
import js.JSConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

case class IDsVM(isLastPage:Boolean,
                    currentPage:Int,
                    ids:Seq[String],
                    count:Int    //stores the number of rows resulting from the query without paging

                   )

object IDsVMFactory{
  def empty = IDsVM(true,1,Seq(),0)
}



case class FieldQuery(field:JSONField, sort:String, sortOrder:Option[Int], filterValue:String, filterOperator:String)

case class EntityTableModel(name:String, kind:String, urlQuery:Option[JSONQuery], rows:Seq[Row], fieldQueries:Seq[FieldQuery],
                            metadata:Option[JSONMetadata], selectedRow:Seq[JSONID], ids: IDsVM, pages:Int, access:TableAccess,
                            lookups:Seq[JSONLookups],query:Option[JSONQuery],geoms: GeoTypes.GeoData,extent:Option[Polygon],public:Boolean,selectedColumns:Seq[JSONField])


case class VMAction(code:String,action: JSONID => Future[Boolean],icon:Option[Icon],label:String,button_class:String = "primary",confirm:Option[String] = None, reloadAfter:Boolean = false)


object EntityTableModel extends HasModelPropertyCreator[EntityTableModel]{
  def empty = EntityTableModel("","",None,Seq(),Seq(),None,Seq(),IDsVMFactory.empty,1, TableAccess(false,false,false),Seq(),None,Seq(),None,false,Seq())
  implicit val blank: Blank[EntityTableModel] =
    Blank.Simple(empty)
}

object FieldQuery extends HasModelPropertyCreator[FieldQuery]
object IDsVM extends HasModelPropertyCreator[IDsVM] {
  def fromIDs(ids:IDs) = IDsVM(
    ids.isLastPage,
    ids.currentPage,
    ids.ids,
    ids.count
  )
}


case class EntityTableViewPresenter(routes:Routes, onSelect:Seq[(JSONField,String)] => Unit = (f => ())) extends ViewFactory[EntityTableState] {




  override def create(): (View, Presenter[EntityTableState]) = {

    val model = ModelProperty.blank[EntityTableModel]

    val presenter = EntityTablePresenter(model,onSelect,routes)
    (EntityTableView(model,presenter,routes),presenter)
  }
}


/*
Failed to decode JSON on
/model/en/v_remark_base/metadata
with error: DecodingFailure(String, List(DownArray, DownField(fields), DownArray, DownField(blocks), DownField(layout)))
 */
case class EntityTablePresenter(model:ModelProperty[EntityTableModel], onSelect:Seq[(JSONField,String)] => Unit, routes:Routes) extends Presenter[EntityTableState] with Logging {


  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._


  private var filterUpdateHandler: Int = 0
//  final private val SKIP_RELOAD_ROWS:Int = 999

  private var fieldListener:Option[Registration] = None

  def addFieldQueryListener() = {
    fieldListener.foreach(_.cancel())
    val listener = model.subProp(_.fieldQueries).listen { fq =>

      logger.info("filterUpdateHandler " + filterUpdateHandler)

      if (filterUpdateHandler != 0) window.clearTimeout(filterUpdateHandler)

      filterUpdateHandler = window.setTimeout(() => {
        reloadRows(1)
      }, 500)
    }
    fieldListener = Some(listener)
  }


  override def handleState(state: EntityTableState): Unit = {
    fieldListener.foreach(_.cancel())
    services.clientSession.loading.set(true)
    services.data.tabularMetadata(state.kind,services.clientSession.lang(),state.entity,state.public).map{ metadata =>
      metadata.static match {
        case false => _handleState(state,metadata)
        case true => {
          services.clientSession.loading.set(false)
          Context.applicationInstance.goTo(
            FormPageState(state.kind,state.entity,"true",state.public),
            true
          )
        }
      }
    }
  }

  private def _handleState(state: EntityTableState,emptyFieldsForm:JSONMetadata): Unit = {

    services.clientSession.loading.set(true)
    logger.info(s"handling Entity table state name=${state.entity}, kind=${state.kind} and query=${state.query}")

    val stateQuery = URLQuery.fromState(state.query)
    val urlQuery:Option[JSONQuery] = URLQuery.fromQueryParameters(Routes.urlParams.get("q")).orElse(stateQuery)
    services.clientSession.setURLQuery(urlQuery.getOrElse(JSONQuery.empty))

    val fields = emptyFieldsForm.fields.filter(field => emptyFieldsForm.tabularFields.contains(field.name))
    val form = emptyFieldsForm.copy(fields = fields)

    val defaultQuery:JSONQuery = JSONQuery.empty.limit(ClientConf.pageLength)

    val queryWithGeom:JSONQuery = services.clientSession.getQueryFor(state.kind,state.entity,urlQuery) match {
      case Some(jsonquery) => jsonquery      //in case a query is already stored in Session
      case _ => urlQuery.getOrElse(defaultQuery)
    }

    val geomFields = emptyFieldsForm.geomFields.map(_.name)
    val query = queryWithGeom.copy(filter = queryWithGeom.filter.filterNot(f => geomFields.contains(f.column)))

    {for{
      access <- { if(!state.public)
        services.rest.tableAccess(form.entity,state.kind)
      else Future.successful(TableAccess(false,false,false))
      }
      specificKind <- { if(!state.public)
        services.rest.specificKind(state.kind, services.clientSession.lang(), state.entity)
      else Future.successful(EntityKind.FORM.kind)
      }
    } yield {


      val m = EntityTableModel(
        name = state.entity,
        kind = specificKind,
        urlQuery = urlQuery,
        rows = Seq(),
        fieldQueries = form.table.map{ field =>

          val operator = query.filter.find(_.column == field.name).flatMap(_.operator).getOrElse(Filter.default(field))
          val rawValue = query.filter.find(_.column == field.name).flatMap(_.value).getOrElse("")
          FieldQuery(
            field = field,
            sort = query.sort.find(_.column == field.name).map(_.order).getOrElse(Sort.IGNORE),
            sortOrder = query.sort.zipWithIndex.find(_._1.column == field.name).map(_._2 + 1),
            filterValue = rawValue,
            filterOperator = operator
          )
        },
        metadata = Some(form),
        selectedRow = Seq(),
        ids = IDsVMFactory.empty,
        pages = Navigation.pageCount(0),
        access = access,
        lookups = Seq(),
        query = Some(query),
        geoms = Seq(),
        extent = None,
        public = state.public,
        selectedColumns = form.table
      )

      //saveIds(IDs(true,1,Seq(),0),query)


      model.set(m)
      model.subProp(_.name).set(state.entity)  //it is not set by the above line
      reloadRows(1)
      addFieldQueryListener()

    }}.recover{ case e => {
      e.printStackTrace()
      services.clientSession.loading.set(false)
    }}
  }

  val tooltipList = ListBuffer[UdashTooltip]()

  override def onClose(): Unit = {
    super.onClose()
    tooltipList.foreach(_.destroy())
  }

  def edit(new_window:Boolean)(id: JSONID) = {
    if (new_window) {
      window.open(routes.edit(id.asString).url(applicationInstance))
    } else {
      Navigate.to(routes.edit(id.asString))
    }
    Future.successful(true)
  }

  def clickOnMap(idString:String) = {
    if(model.get.access.update)
      Navigate.to(routes.edit(idString))
    else
      Navigate.to(routes.show(idString))
  }

  model.subProp(_.extent).listen { extent =>
    reloadRows(1)
  }

  def show(new_window:Boolean)(id: JSONID) =  {
    if (new_window) {
      window.open(routes.show(id.asString).url(applicationInstance))
    } else {
      Navigate.to(routes.show(id.asString))
    }
    Future.successful(true)
  }

  def delete(id: JSONID) =  {

    model.get.metadata.map(_.name) match {
      case Some(name) => services.rest.delete(model.get.kind, services.clientSession.lang(),name,id).map{ count =>
        Notification.add("Deleted " + count.count + " rows")
        true
      }.recover{case _:Throwable => true}
      case None => Future.successful(true)
    }

  }



  def actions(new_window:Boolean):Seq[VMAction] = {

    val metadata = model.subProp(_.metadata).get
    metadata.toSeq.flatMap(_.action.table(model.get.access)).map { ta =>
      ta.action match {
        case DeleteAction => VMAction("delete",delete,Some(Icons.x),Labels.entity.delete,"danger",Some(Labels.entity.confirmDelete),reloadAfter = true)
        case EditAction => VMAction("edit",edit(new_window),Some(Icons.pencil_square),Labels.entity.edit)
        case ShowAction => VMAction("show",show(new_window),Some(Icons.arrow_up_square),Labels.entity.show)
        case NoAction => {
          def rowEv(id: JSONID):  Future[Boolean] = {

            for {
              data <- getObj(id)
              _ <- ta.executeFunction match {
                case Some(f) => {
                  services.rest.execute(f, services.clientSession.lang(), data).map { result =>
                    result.errorMessage match {
                      case Some(value) => {
                        Notification.add(value)
                        services.clientSession.loading.set(false)
                        false
                      }
                      case None => {
                        services.clientSession.loading.set(false)
                        true
                      }
                    }
                  }
                }
                case None => Future.successful(())
              }
            } yield {
              val url = Routes.getUrl(ta, data, metadata.get.kind, metadata.get.name, Some(id.asString), model.get.access.update)
              (ta.target, new_window, url) match {
                case (_,_, None) => ()
                case (Self, false, Some(url)) => Navigate.toUrl(url)
                case (_,_, Some(url)) =>  window.open(url)
              }
              true
            }

          }
          VMAction(ta.label,rowEv,None,Labels(ta.label),confirm = ta.confirmText, reloadAfter = ta.reload)
        }

      }
    }

  }

  def hasGeometry():Boolean = {
    val metadata = model.get.metadata
    metadata.toSeq.flatMap(_.fields).filter(metadata.toSeq.flatMap(_.exportFields) contains _.name).exists(_.`type`==JSONFieldTypes.GEOMETRY)
  }

  def saveIds(ids: IDs, query:JSONQuery) = {
    services.clientSession.setQueryFor(model.get.kind,model.get.name,model.get.urlQuery,query)
    services.clientSession.setIDs(ids)
  }

  def urlOnlyFilter:Option[JSONQuery] = {
      for {
        m <- model.subProp(_.metadata).get
        uq <-  model.subProp(_.urlQuery).get
      } yield {
        val qf = uq.filter.filterNot(f => m.tabularFields.contains(f.column))
        val qs = uq.sort.filterNot(s => m.tabularFields.contains(s.column))
        JSONQuery.empty
          .filterWith(qf: _*)
          .sortWith(qs: _*)
      }
  }

  def query(extent:Option[Polygon]):JSONQuery = {
    val fieldQueries = model.subProp(_.fieldQueries).get




    val sort = fieldQueries.filter(_.sort != Sort.IGNORE).sortBy(_.sortOrder.getOrElse(-1)).map(s => JSONSort(s.field.name, s.sort)).toList

    val filter = fieldQueries.filter(_.filterValue != "").map{ f =>
      JSONQueryFilter.withValue(f.field.name,Some(f.filterOperator),f.filterValue)
    }.toList

    val qFields = JSONQuery(filter, sort, None)
    val q = (model.get.metadata,extent) match {
      case (Some(metadata),Some(ext)) => qFields.withExtent(metadata,ext)
      case _ => qFields
    }

    urlOnlyFilter match {
      case Some(uq) => q.filterWith(q.filter ++ uq.filter:_*).sortWith(q.sort ++ uq.sort:_*)
      case None => q
    }




  }

  var reloadCount = 0 // avoid out of order


  def defaultClose = model.subProp(_.metadata).get.exists(_.params.exists(_.js("mapClosed") == Json.True))
  def loadGeoms(extent:Option[Polygon] = None) = {
    model.get.metadata.foreach{ m =>
      Future.sequence(m.geomFields.map{ f =>
        val tableEntity = m.view.getOrElse(m.entity)
        services.rest.geoData(EntityKind.ENTITY.kind, services.clientSession.lang(), tableEntity, f.name, GeoDataRequest(query(extent).limit(10000000),m.keys),model.subProp(_.public).get)

      }).foreach{ geoms =>
        model.subProp(_.geoms).set(geoms.flatten)
      }

    }
  }

  def reloadRows(page:Int): Future[Unit] = {

    reloadCount = reloadCount + 1
    val currentCount = reloadCount

    services.clientSession.loading.set(true)

    val extent = model.subProp(_.extent).get

    logger.info(s"reloading rows page: $page")
    logger.info("filterUpdateHandler "+filterUpdateHandler)
    val qOrig = query(extent)
    val newQuery = !model.subProp(_.query).get.contains(qOrig)
    model.subProp(_.query).set(Some(qOrig))
    val q = qOrig.copy(paging = Some(JSONQueryPaging(ClientConf.pageLength, page)))

    //start request in parallel
    val csvRequest = services.data.list(model.subProp(_.kind).get, services.clientSession.lang(), model.subProp(_.name).get, q,model.subProp(_.public).get)
    val idsRequest =  services.rest.ids(model.get.kind, services.clientSession.lang(), model.get.name, q,model.subProp(_.public).get)
    if(hasGeometry() && !defaultClose) {
      loadGeoms(extent)
    }

    def lookupReq(csv:Seq[Row]) = model.subProp(_.metadata).get match {
      case Some(m) => {
        val fields = m.tableLookupFields.map(_.name)
        if (fields.length > 0)
          services.rest.lookups(model.get.kind, services.clientSession.lang(), model.get.name, JSONLookupsRequest(fields, qOrig), model.get.public)
        else
          Future.successful(Seq())

      }
      case None => Future.successful(Seq())
    }

    val r = for {
      csv <- csvRequest
      ids <- idsRequest
      lookups <- if(newQuery) lookupReq(csv) else Future.successful( model.subProp(_.lookups).get)
    } yield {
      if(currentCount == reloadCount) {
        model.subProp(_.lookups).set(lookups)
        model.subProp(_.rows).set(csv)
        model.subProp(_.ids).set(IDsVM.fromIDs(ids))
        model.subProp(_.pages).set(Navigation.pageCount(ids.count))
        saveIds(ids, q)
        services.clientSession.loading.set(false)
      }
    }

    r.recover{ t =>
      t.printStackTrace()
      services.clientSession.loading.set(false)
    }

    r

  }




  def sort(_fieldQuery: ReadableProperty[Option[FieldQuery]]) = (e:Event) => {
    e.preventDefault()

    if(_fieldQuery.get.isDefined) {

      val fieldQuery = _fieldQuery.get.get

      val next = Sort.next(fieldQuery.sort)

      val fieldQueries = model.subProp(_.fieldQueries).get

      val newFieldQueries = fieldQueries.map { m =>

        next match {
          case Sort.IGNORE => // drop order
          case Sort.ASC => // add order
          case _ => // keep order
        }


        m.field.name == fieldQuery.field.name match {
          case false => next match {
            case Sort.IGNORE if m.sortOrder.isDefined && fieldQuery.sortOrder.isDefined && m.sortOrder.get > fieldQuery.sortOrder.get => m.copy(sortOrder = m.sortOrder.map(_ - 1))
            case _ => m
          }
          case true => {
            next match {
              case Sort.IGNORE => m.copy(sort = next, sortOrder = None) // drop order
              case Sort.ASC => m.copy(sort = next, sortOrder = Some(fieldQueries.map(_.sortOrder.getOrElse(0)).max + 1)) // add order
              case _ => m.copy(sort = next) // keep order
            }
          }
        }
      }

      model.subProp(_.fieldQueries).set(newFieldQueries)
    }
  }

  def toggleSelection(row: => Row) = (e:Event) => {

    row.id.foreach { id =>
      val currentSel = model.subProp(_.selectedRow).get
      if (currentSel.contains(id)) {
        model.subProp(_.selectedRow).set(currentSel.filterNot(_ == id))
      } else {
        //not used yet, was for parent child relationships
        onSelect(model.get.fieldQueries.map(_.field).zip(row.data))
        model.subProp(_.selectedRow).set(currentSel ++ Seq(id))
      }
    }
    e.preventDefault()
  }

  def nextPage() = (e:Event) => {
    if(!model.subProp(_.ids.isLastPage).get) {
      reloadRows(model.subProp(_.ids.currentPage).get + 1)
    }
    e.preventDefault()
  }
  def prevPage() = (e:Event) => {
    if(model.subProp(_.ids.currentPage).get > 1) {
      reloadRows(model.subProp(_.ids.currentPage).get - 1)
    }
    e.preventDefault()
  }


  val downloadCSV = (e:Event) => {
    download("csv")
    e.preventDefault()
  }

  val downloadSHP = (e:Event) => {
    download("shp")
    e.preventDefault()
  }

  val downloadGeoPackage = (e: Event) => {
    download("gpkg")
    e.preventDefault()
  }

  val downloadXLS = (e:Event) => {
    download("xlsx")
    e.preventDefault()
  }

  private def download(format:String) = {

    val kind = EntityKind(model.subProp(_.kind).get).entityOrForm
    val modelName =  model.subProp(_.name).get
    val exportFields = model.get.metadata.map(_.exportFields).getOrElse(Seq())
    val fields = model.get.metadata.map(_.fields).getOrElse(Seq())


    val queryNoLimits = query(None).copy(paging = None)


    val url = Routes.apiV1(
      s"/$kind/${services.clientSession.lang()}/$modelName/$format?fk=${ExportMode.RESOLVE_FK}&fields=${exportFields.mkString(",")}&q=${URIUtils.encodeURI(queryNoLimits.asJson.noSpaces)}".replaceAll("\n","")
    )
    logger.info(s"downloading: $url")
    dom.window.open(url)
  }

  def getObj(id:JSONID):Future[Json] = {
    services.rest.get(model.get.kind, services.clientSession.lang(), model.get.name,id)
  }

  def resetFilters() = {
    model.subProp(_.extent).set(None)
    model.subProp(_.fieldQueries).set(model.subProp(_.fieldQueries).get.map(_.copy(filterValue = "")))
  }

  def selectAll() = {
    val count = model.subProp(_.ids).get.count
    val q = model.subProp(_.query).get match {
      case Some(value) => value.limit(count)
      case None => JSONQuery.empty.limit(count)
    }

    for {
     ids <- services.rest.ids(model.get.kind, services.clientSession.lang(), model.get.name, q, model.subProp(_.public).get)
    } yield model.subProp(_.selectedRow).set(ids.ids.flatMap(i => JSONID.fromString(i,model.subProp(_.metadata).get.get)))
  }

  def resetSelection() = {
    model.subProp(_.selectedRow).set(Seq())
  }

  def isFiltered(query:Option[JSONQuery]):Boolean = query.exists{ q =>
    model.subProp(_.metadata).get match {
      case Some(m) => q.filter.exists(f => m.tabularFields.contains(f.column))
      case None => q.filter.nonEmpty
    }
  }


}

case class EntityTableView(model:ModelProperty[EntityTableModel], presenter:EntityTablePresenter, routes:Routes) extends View with Logging {
  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.client.Context.Implicits._




  def labelTitle(m:Option[JSONMetadata]) = {
    val name = m.map(_.label).getOrElse(model.get.name)
    span(name).render
  }

  def filterOptions(metadata:Option[JSONMetadata], field:String, operator: Property[String]) = {


    def label = (id:String) => id match {
      case Filter.FK_NOT => StringFrag(Labels.filter.not)
      case Filter.FK_EQUALS => StringFrag(Labels.filter.equals)
      case Filter.FK_LIKE => StringFrag(Labels.filter.contains)
      case Filter.FK_DISLIKE => StringFrag(Labels.filter.without)
      case Filter.LIKE => StringFrag(Labels.filter.contains)
      case Filter.DISLIKE => StringFrag(Labels.filter.without)
      case Filter.BETWEEN => StringFrag(Labels.filter.between)
      case Filter.< => StringFrag(Labels.filter.lt)
      case Filter.> => StringFrag(Labels.filter.gt)
      case Filter.<= => StringFrag(Labels.filter.lte)
      case Filter.>= => StringFrag(Labels.filter.gte)
      case Filter.EQUALS => StringFrag(Labels.filter.equals)
      case Filter.IN => StringFrag(Labels.filter.in)
      case Filter.NONE => StringFrag(Labels.filter.none)
      case Filter.NOTIN => StringFrag(Labels.filter.notin)
      case Filter.NOT => StringFrag(Labels.filter.not)
      case _ => StringFrag(id)
    }

    val options = SeqProperty{
      metadata.toSeq.flatMap(_.fields).find(_.name == field).toSeq.flatMap(f => UI.enabledFilters(Filter.options(f)))
    }

    Select(operator, options)(label,ClientConf.style.fullWidth,ClientConf.style.filterTableSelect)

  }



  def filterField(filterValue: Property[String], field:Option[JSONField], filterOperator:String,nested:Binding.NestedInterceptor):Modifier = {

    filterValue.listen(v => logger.info(s"Filter for ${field.map(_.name)} changed in: $v"))



    def filterFieldStd = field.map(_.`type`) match {
      case Some(JSONFieldTypes.TIME) => DateTimeWidget.Time(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null)).edit(nested)
      case Some(JSONFieldTypes.DATE) => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
      case Some(JSONFieldTypes.DATETIME) => ClientConf.filterPrecisionDatetime match{
        case JSONFieldTypes.DATE => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
        case _ => DateTimeWidget.DateTime(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
      }
      case Some(JSONFieldTypes.NUMBER) | Some(JSONFieldTypes.INTEGER) if field.flatMap(_.widget).contains(WidgetsNames.integerDecimal2) && !Seq(Filter.BETWEEN, Filter.IN, Filter.NOTIN).contains(filterOperator) => {
        if(Try(filterValue.get.toDouble).toOption.isEmpty) filterValue.set("")
        val properyNumber = Property("")
        filterValue.sync(properyNumber)(_.toDoubleOption.map(_ / 100).map(_.toString).getOrElse(""),_.toDoubleOption.map(_ * 100).map(_.toString).getOrElse("") )
        NumberInput(properyNumber)(ClientConf.style.fullWidth)
      }
      case Some(JSONFieldTypes.NUMBER) | Some(JSONFieldTypes.INTEGER) if field.flatMap(_.lookup).isEmpty && !Seq(Filter.BETWEEN, Filter.IN, Filter.NOTIN).contains(filterOperator) => {
        if(Try(filterValue.get.toDouble).toOption.isEmpty) filterValue.set("")
        NumberInput(filterValue)(ClientConf.style.fullWidth)
      }
      case _ => TextInput(filterValue)(ClientConf.style.fullWidth)
    }

    def filterFieldLookup(lookup:JSONFieldLookup) = {
      def choises(lookups:JSONLookups):Seq[InputChoice] = lookup match {
        case JSONFieldLookupRemote(lookupEntity, map, lookupQuery) => {
           lookups.lookups.map(l => InputChoice(l.value,l.id.string))
        }
        case JSONFieldLookupExtractor(extractor) => Seq()
        case JSONFieldLookupData(data) => data.map(x => InputChoice(x.value,x.id.string))
      }

      val el = select().render


      val observer = new MutationObserver({ (mutations, observer) =>
        observer.disconnect()
        val options = PartialOptions()
          .setRemoveItemButton(true)
          .setShouldSort(false)
          .setItemSelectText("")
        val choicesJs = new ch.wsl.typings.choicesJs.mod.default(el, options)
        el.addEventListener("change", (e: Event) => {
          (choicesJs.getValue(true): Any) match {
            case list: js.Array[String] => println(list)
            case a: String => filterValue.set(a)
            case _ => filterValue.set("")
          }
        })

        model.subProp(_.lookups).listen({l =>
          l.find(_.fieldName == field.get.name).foreach{ fl =>
            choicesJs.clearChoices()
            val c = choises(fl)
            choicesJs.asInstanceOf[js.Dynamic].setChoices(c.toJSArray)
            if(filterValue.get.nonEmpty) {
              choicesJs.setChoiceByValue(filterValue.get)
            }
          }

        },true)

        filterValue.listen{ fv =>
          if(fv.isEmpty && choicesJs.getValue(true).toString.nonEmpty) {
            choicesJs.clearStore()
          }
        }

      })
      observer.observe(document,MutationObserverInit(childList = true, subtree = true))
      el
    }

    field.flatMap(_.lookup) match {
      case Some(value) => filterFieldLookup(value)
      case None => filterFieldStd
    }

  }

  var map:Option[Div] = None

  def showMap(metadata:JSONMetadata) = (show:ReadableProperty[Boolean]) => showIf(show){
    if(presenter.hasGeometry()) {

      if(model.subProp(_.geoms).get.isEmpty)
        presenter.loadGeoms(model.subProp(_.extent).get)

      if(window.innerWidth < 600)  { // is mobile
        map = Some(div(height := (window.innerHeight - 50).px).render)
      } else {
        map = Some(div(height := (window.innerHeight - 105).px).render)
      }


      val observer = new MutationObserver({ (mutations, observer) =>
        map match {
          case Some(m) => if (document.contains(m) && m.offsetHeight > 0) {
            observer.disconnect()
            new MapList(m,metadata,presenter.model.subProp(_.geoms),presenter.clickOnMap,model.subProp(_.extent))
          }
          case None => observer.disconnect()
        }
      })
      observer.observe(document, MutationObserverInit(childList = true, subtree = true))
      map.get
    } else {
      map = None
      div().render
    }
  }



  override def getTemplate: generic.Modifier[Element] = div(
    produceWithNested(model.subProp(_.metadata)) { (metadata,nested) =>
      if (presenter.hasGeometry()) {
        div(new TwoPanelResize(presenter.defaultClose)(showMap(metadata.getOrElse(JSONMetadata.stub)),mainContent(metadata,nested))).render
      } else {
        div(mainContent(metadata,nested)).render
      }
    }
  )


  def mainActions(metadata:Option[JSONMetadata]) = produceWithNested(model.subProp(_.access)) { (a, releaser) =>

    val adminActions = if(services.clientSession.isAdmin() && model.subProp(_.kind).get == EntityKind.FORM.kind) {
      Seq(FormAction(NoAction,Primary,Some(s"/box/box-form/form/row/true/form_uuid::${metadata.map(_.objId).getOrElse("")}"),Labels("Edit UI")))
    } else Seq()

    Seq(
      div(ClientConf.style.noMobile)(
        releaser(produce(model.subProp(_.name)) { m =>
          div({
            val out: Seq[Modifier] = (metadata.toSeq.flatMap(_.action.topTable(a)) ++ adminActions).map { ta =>

              val importance: StyleA = ta.importance match {
                case Primary => ClientConf.style.boxButtonImportant
                case Danger => ClientConf.style.boxButtonDanger
                case Std => ClientConf.style.boxButton
              }

              ta.action match {
                case SaveAction => ???
                case EditAction => ???
                case ShowAction => ???
                case CopyAction => ???
                case RevertAction => ???
                case DeleteAction => ???
                case NoAction => {
                  button(importance,
                    id := TestHooks.actionButton(ta.label),
                    onclick :+= { (e: Event) =>
                      val execute = ta.confirmText match {
                        case Some(msg) => window.confirm(msg)
                        case None => true
                      }
                      if (execute) {
                        val function = ta.executeFunction match {
                          case Some(f) => services.rest.execute(f, services.clientSession.lang(), Json.Null).map { result =>
                            result.errorMessage match {
                              case Some(value) => {
                                Notification.add(value)
                                services.clientSession.loading.set(false)
                                false
                              }
                              case None => true
                            }
                          }
                          case None => Future.successful(())
                        }
                        function.foreach { _ =>
                          Routes.getUrl(ta, Json.Null, model.subProp(_.kind).get, model.get.name, None, a.insert) match {
                            case Some(url) => Navigate.toUrl(url)
                            case None => {
                              Context.applicationInstance.reload()
                            }
                          }
                        }
                      }

                      e.preventDefault()
                    })(Labels(ta.label))
                }
                case BackAction => ???
              }
            }
            out

          }).render
        })
      ),
      div(
        releaser(produce(model.subProp(_.name)) { m =>

          button(id := TestHooks.mobileTableAdd, ClientConf.style.mobileBoxAction, Navigate.click(Routes(model.subProp(_.kind).get, m,model.subProp(_.public).get).add()))(i(UdashIcons.FontAwesome.Solid.plus)).render
        })
      ),
    ).render
  }

  def actionButton(els: => Seq[JSONID],mod:Modifier*)(action:VMAction) = {
    val b = a(
      mod,
      cls := s"action ${action.button_class} " + TestHooks.tableActionButton(action.code),
      onclick :+= { (e:Event) =>
        val execute = action.confirm match {
          case Some(msg) => window.confirm(msg)
          case None => true
        }
        if (execute) {
          services.clientSession.loading.set(true)
          Future.sequence(els.map(action.action)).foreach{ _ =>
            if(action.reloadAfter) {
              presenter.reloadRows(model.subProp(_.ids.currentPage).get)
            }
            services.clientSession.loading.set(false)
          }
        }
        e.preventDefault()
      }
    )(action.icon.getOrElse(action.label)).render

    if(action.icon.isDefined) {
      presenter.tooltipList.addOne(
        UdashTooltip(
          trigger = Seq(UdashTooltip.Trigger.Hover),
          delay = UdashTooltip.Delay(250 millis, 0 millis),
          placement = UdashTooltip.Placement.Auto,
          title = action.label
        )(b)
      )
    }

    b
  }

  def rowActions(el:ReadableProperty[Row]) = {

    div(ClientConf.style.tableCellActions)(
      presenter.actions(false).map(actionButton(el.get.id.toSeq))
    )


  }

  def tableContent(metadata:Option[JSONMetadata]) = {
    produce(model.subProp(_.selectedColumns)) { columns =>
      val table = UdashTable(model.subSeq(_.rows))(

        headerFactory = Some(_ => {
          frag(
            tr(
              th(ClientConf.style.smallCells, verticalAlign.middle, colspan := 2)(
                mainActions(metadata)
              ),
              columns.filterNot(_.`type` == JSONFieldTypes.GEOMETRY).map { field =>
                val fieldQuery: ReadableProperty[Option[FieldQuery]] = model.subProp(_.fieldQueries).transform(_.find(_.field.name == field.name))
                val title: ReadableProperty[String] = fieldQuery.transform(_.flatMap(_.field.label).getOrElse(field.name))
                val sort: ReadableProperty[String] = fieldQuery.transform(_.map(x => x.sort).getOrElse(""))
                val order: ReadableProperty[String] = fieldQuery.transform(_.flatMap(_.sortOrder).map(_.toString).getOrElse(""))

                th(ClientConf.style.smallCells, verticalAlign.middle, draggable := true)(
                  a(
                    onclick :+= presenter.sort(fieldQuery),
                    span(bind(title), ClientConf.style.tableHeader), " ",
                    span(whiteSpace.nowrap, span(produce(sort) {
                      case Sort.ASC => Icons.asc.render
                      case Sort.DESC => Icons.desc.render
                      case _ => frag().render
                    }), " ", bind(order))
                  )
                ).render
              }
            ),
            tr(
              td(ClientConf.style.smallCells, colspan := 2)(Labels.entity.filters),
              columns.filterNot(_.`type` == JSONFieldTypes.GEOMETRY).map { _field =>
                val fieldQuery: Property[Option[FieldQuery]] = model.subProp(_.fieldQueries).bitransform(_.find(_.field.name == _field.name)) { el =>
                  model.subProp(_.fieldQueries).get.map { old =>
                    if (old.field.name == _field.name && el.isDefined) el.get else old
                  }
                }
                val filterValue: Property[String] = fieldQuery.bitransform(_.map(_.filterValue).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterValue = value)))
                val operator: Property[String] = fieldQuery.bitransform(_.map(_.filterOperator).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterOperator = value)))

                td(ClientConf.style.smallCells)(
                  filterOptions(metadata, _field.name, operator),
                  produceWithNested(operator) { (op, nested) =>
                    div(position.relative, filterField(filterValue, Some(_field), op, nested)).render
                  }
                ).render

              }
            )
          ).render
        }),
        rowFactory = (el, nested) => {
          val selected = model.subProp(_.selectedRow).transform(_.exists(i => el.get.id.contains(i)))

          val row = tr(
            id := ElementId.tableRow(el.get.id.map(_.asString).getOrElse("")),
            ClientConf.style.rowStyle, onclick :+= presenter.toggleSelection(el.get),
            td(ClientConf.style.smallCells)(
              Offline(el.transform(_.isLocal)),
            ),
            td(ClientConf.style.smallCells)(
              rowActions(el)
            ),
            for {col <- columns} yield {

              val value = el.get.field(col.name)
              value match {
                case Some(_) if col.`type` == JSONFieldTypes.GEOMETRY => None
                case Some(v) => Some(td(ClientConf.style.smallCells)(TableFieldsRenderer(
                  v.string,
                  col,
                  model.subProp(_.lookups).get
                )).render)
                case None => Some(td().render)
              }
            }
          ).render

          selected.listen({
            case true => row.classList.add("selected")
            case false => row.classList.remove("selected")
          }, true)

          row
        }
      ).render

      def labelExtractor(el:Element):String = {
        val head = if(el.classList.contains(ClientConf.style.tableHeader.className.value)) {
          el
        } else {
          el.querySelector(ClientConf.style.tableHeader.selector)
        }
        head.innerText
      }

      new TableColumnDrag(table, labelExtractor,e => {
        BrowserConsole.log(e)
        val oldPosition = e.dataTransfer.getData("text")
        val newPosition = labelExtractor(e.target.asInstanceOf[HTMLElement])

        val sc = model.subProp(_.selectedColumns)

        sc.set(sc.get.flatMap{ f =>
          if(f.title == oldPosition) Seq()
          else if(f.title == newPosition) metadata.flatMap(_.table.find(_.title == oldPosition)) ++ Seq(f)
          else Seq(f)
        })

      })
      table
    }
  }

  def mainContent(metadata:Option[JSONMetadata],nested:Binding.NestedInterceptor): scalatags.generic.Modifier[Element] = {

    val columnSelector = {
      var modal:Option[UdashModal] = None
      val localModel = Property(model.get.selectedColumns)
      modal = Some(UdashModal(
        Some(Size.Small).toProperty,
        backdrop = Property(BackdropType.Static)
      )(
        headerFactory = Some(_ => div(Labels.table.column_selection).render),
        bodyFactory = Some { nested =>
          div(
              metadata.toSeq.flatMap(_.table).filterNot(_.`type` == JSONFieldTypes.GEOMETRY).map{ c =>
                div(
                  Checkbox(localModel.bitransform(_.contains(c)){
                    case true => localModel.get ++ Seq(c)
                    case false => localModel.get.filterNot(_ == c)
                  })()," ",c.title
                )
              }

          ).render
        },
        footerFactory = Some { _ =>
          div(
            button(`type` := "button", onclick :+= {(e:Event) =>
              model.subProp(_.selectedColumns).set(localModel.get)
              modal.foreach(_.hide())
            }, ClientConf.style.boxButton, Labels.form.save)
          ).render
        }
      ))
      div(
        button(`type` := "button", onclick :+= {(e:Event) =>
          localModel.set(model.subProp(_.selectedColumns).get)
          modal.foreach(_.show())
        }, ClientConf.style.boxButton, Icons.dots),
        modal.get
      )
    }

    val pagination = {

      div(ClientConf.style.navigationBlock,
        Navigation.button(model.subProp(_.ids.currentPage).transform(_ != 1),() => presenter.reloadRows(1),i(UdashIcons.FontAwesome.Solid.fastBackward)),
        Navigation.button(model.subProp(_.ids.currentPage).transform(_ != 1),() => presenter.reloadRows(model.subProp(_.ids.currentPage).get -1),i(UdashIcons.FontAwesome.Solid.caretLeft)),
        span(
          " " + Labels.navigation.page + " ",
          bind(model.subProp(_.ids.currentPage)),
          " " + Labels.navigation.of + " ",
          bind(model.subProp(_.pages)),
          " "
        ),
        Navigation.button(model.subModel(_.ids).subProp(_.isLastPage).transform(!_),() => presenter.reloadRows(model.subProp(_.ids.currentPage).get + 1),i(UdashIcons.FontAwesome.Solid.caretRight)),
        Navigation.button(model.subModel(_.ids).subProp(_.isLastPage).transform(!_),() => presenter.reloadRows(model.subProp(_.pages).get),i(UdashIcons.FontAwesome.Solid.fastForward)),

      )
    }


      div(
        div(ClientConf.style.spaceBetween,
          div(
            h3(ClientConf.style.noMargin,ClientConf.style.formTitle, labelTitle(metadata))
          ),
          div(display.flex,flexDirection.row,alignItems.center,
            div( Labels.navigation.recordFound," ",nested(bind(model.subProp(_.ids.count)))),
            nested(showIf(model.subProp(_.query).transform(presenter.isFiltered)){
                a(ClientConf.style.chipLink,Labels.navigation.recordsFiltered," \uD83D\uDDD9", onclick :+= ((e:Event) => {
                  presenter.resetFilters()
                  e.preventDefault()
                })
              ).render
            }),
            a(ClientConf.style.chipLink,Labels.navigation.selectAll, onclick :+= ((e:Event) => {
              presenter.selectAll()
              e.preventDefault()
            }))
          ),
          div(
            nested(showIf(model.subProp(_.selectedRow).transform(_.nonEmpty)){
              div(
                Labels.navigation.recordsSelected,nested(bind(model.subProp(_.selectedRow).transform(_.length))),
                presenter.actions(true).map(actionButton(model.subProp(_.selectedRow).get,ClientConf.style.chipLink)),
                a(ClientConf.style.chipLink,Labels.navigation.removeSelection," \uD83D\uDDD9", onclick :+= ((e:Event) => {
                  presenter.resetSelection()
                  e.preventDefault()
                })),
              ).render
            })
          ),
          div( display.flex,
            columnSelector,
            pagination.render,
          )

        ),
        div(id := "box-table", ClientConf.style.fullHeightMax,ClientConf.style.tableHeaderFixed,
          tableContent(metadata),
          button(`type` := "button", onclick :+= presenter.downloadCSV, ClientConf.style.boxButton, Labels.entity.csv),
          button(`type` := "button", onclick :+= presenter.downloadXLS, ClientConf.style.boxButton, Labels.entity.xls),
          if (presenter.hasGeometry()) {
            Seq(
              //button(`type` := "button", onclick :+= presenter.downloadSHP, ClientConf.style.boxButton, Labels.entity.shp),
              button(`type` := "button", onclick :+= presenter.downloadGeoPackage, ClientConf.style.boxButton, Labels.entity.geoPackage)
            )
          } else frag(),
          showIf(model.subProp(_.fieldQueries).transform(_.size == 0)) {
            p("loading...").render
          },
          br, br
        ),
        Debug(model)
      ).render

  }


}
