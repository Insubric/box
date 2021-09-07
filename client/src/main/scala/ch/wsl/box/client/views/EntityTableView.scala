package ch.wsl.box.client.views

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.{Context, EntityFormState, EntityTableState, FormPageState}
import ch.wsl.box.client.services.{ClientConf, Labels, Navigate, Navigation, Notification}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.utils.URLQuery
import ch.wsl.box.client.views.components.widget.DateTimeWidget
import ch.wsl.box.client.views.components.{Debug, TableFieldsRenderer}
import ch.wsl.box.model.shared.EntityKind.VIEW
import ch.wsl.box.model.shared.{JSONQuery, _}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.udash._
import io.udash.bootstrap.{BootstrapStyles, UdashBootstrap}
import io.udash.bootstrap.table.UdashTable
import io.udash.properties.single.Property
import io.udash.utils.Registration
import org.scalajs.dom
import scalacss.ScalatagsCss._
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{Element, Event, KeyboardEvent, window}
import scalacss.internal.Pseudo.Lang
import scalatags.JsDom.all.a
import scribe.Logging

import scala.concurrent.Future
import scala.scalajs.js.URIUtils
import scala.util.Try

case class IDsVM(isLastPage:Boolean,
                    currentPage:Int,
                    ids:Seq[String],
                    count:Int    //stores the number of rows resulting from the query without paging
                   )

object IDsVMFactory{
  def empty = IDsVM(true,1,Seq(),0)
}

case class Row(data: Seq[String])

case class FieldQuery(field:JSONField, sort:String, sortOrder:Option[Int], filterValue:String, filterOperator:String)

case class EntityTableModel(name:String, kind:String, urlQuery:Option[JSONQuery], rows:Seq[Row], fieldQueries:Seq[FieldQuery],
                            metadata:Option[JSONMetadata], selectedRow:Option[Row], ids: IDsVM, pages:Int, access:TableAccess)


object EntityTableModel extends HasModelPropertyCreator[EntityTableModel]{
  def empty = EntityTableModel("","",None,Seq(),Seq(),None,None,IDsVMFactory.empty,1, TableAccess(false,false,false))
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
    services.rest.tabularMetadata(state.kind,services.clientSession.lang(),state.entity).map{ metadata =>
      metadata.static match {
        case false => _handleState(state,metadata)
        case true => {
          services.clientSession.loading.set(false)
          Context.applicationInstance.goTo(
            FormPageState(state.kind,state.entity,"true",false),
            true
          )
        }
      }
    }
  }

  private def _handleState(state: EntityTableState,emptyFieldsForm:JSONMetadata): Unit = {

    services.clientSession.loading.set(true)
    logger.info(s"handling Entity table state name=${state.entity}, kind=${state.kind} and query=${state.query}")

    val urlQuery:Option[JSONQuery] = URLQuery(state.query,emptyFieldsForm)
    services.clientSession.setURLQuery(urlQuery)

    val fields = emptyFieldsForm.fields.filter(field => emptyFieldsForm.tabularFields.contains(field.name))
    val form = emptyFieldsForm.copy(fields = fields)

    val defaultQuery:JSONQuery = JSONQuery.empty.limit(ClientConf.pageLength)

    val query:JSONQuery = services.clientSession.getQueryFor(state.kind,state.entity,urlQuery) match {
      case Some(jsonquery) => jsonquery      //in case a query is already stored in Session
      case _ => urlQuery.getOrElse(defaultQuery)
    }

    {for{
      access <- services.rest.tableAccess(form.entity,state.kind)
      specificKind <- services.rest.specificKind(state.kind, services.clientSession.lang(), state.entity)
    } yield {


      val m = EntityTableModel(
        name = state.entity,
        kind = specificKind,
        urlQuery = urlQuery,
        rows = Seq(),
        fieldQueries = form.tabularFields.flatMap(x => form.fields.find(_.name == x)).map{ field =>

          val operator = query.filter.find(_.column == field.name).flatMap(_.operator).getOrElse(Filter.default(field))
          val rawValue = query.filter.find(_.column == field.name).map(_.value).getOrElse("")
          FieldQuery(
            field = field,
            sort = query.sort.find(_.column == field.name).map(_.order).getOrElse(Sort.IGNORE),
            sortOrder = query.sort.zipWithIndex.find(_._1.column == field.name).map(_._2 + 1),
            filterValue = rawValue,
            filterOperator = operator
          )
        },
        metadata = Some(form),
        selectedRow = None,
        ids = IDsVMFactory.empty,
        pages = Navigation.pageCount(0),
        access = access
      )

      saveIds(IDs(true,1,Seq(),0),query)


      model.set(m)
      model.subProp(_.name).set(state.entity)  //it is not set by the above line
      reloadRows(1)
      addFieldQueryListener()

    }}.recover{ case e => {
      e.printStackTrace()
      services.clientSession.loading.set(false)
    }}
  }


  private def extractID(row:Seq[String], fields:Seq[String], keys:Seq[String]):JSONID = {
    val map = for{
      key <- keys
      (field,i) <- fields.zipWithIndex if field == key
    } yield {
      key -> row.lift(i).getOrElse("")
    }
    JSONID.fromMap(map.toMap)
  }


  def ids(el:Row): JSONID = extractID(el.data,model.subProp(_.metadata).get.toSeq.flatMap(_.tabularFields),model.subProp(_.metadata).get.toSeq.flatMap(_.keys))

  def edit(el: => Row) = (e:Event) => {
    val k = ids(el)
    val newState = routes.edit(k.asString)
    Navigate.to(newState)
    e.preventDefault()
  }

  def show(el: => Row) = (e:Event) => {
    val k = ids(el)
    val newState = routes.show(k.asString)
    Navigate.to(newState)
    e.preventDefault()
  }

  def delete(el: => Row) = (e:Event) => {
    val k = ids(el)
    val confim = window.confirm(Labels.entity.confirmDelete)
    if(confim) {
      model.get.metadata.map(_.name).foreach { name =>
        services.rest.delete(model.get.kind, services.clientSession.lang(),name,k).map{ count =>
          Notification.add("Deleted " + count.count + " rows")
          reloadRows(model.get.ids.currentPage)
        }
      }
    }
    e.preventDefault()
  }

  def saveIds(ids: IDs, query:JSONQuery) = {
    services.clientSession.setQueryFor(model.get.kind,model.get.name,model.get.urlQuery,query)
    services.clientSession.setIDs(ids)
  }

  private def encodeFk(fields:Seq[JSONField],query:JSONQuery):JSONQuery = {

    def getFieldLookup(name:String):Seq[JSONLookup] = fields.find(_.name == name).toSeq.flatMap(_.lookup).flatMap(_.lookup)

    val filters = query.filter.map{ field =>
      field.operator match {
        case Some(Filter.FK_LIKE) => {
          val ids = getFieldLookup(field.column)
            .filter(_.value.toLowerCase.contains(field.value.toLowerCase()))
            .map(_.id.string)
          JSONQueryFilter(field.column,Some(Filter.IN),ids.mkString(","))
        }
        case Some(Filter.FK_DISLIKE) => {
          val ids = getFieldLookup(field.column)
            .filter(_.value.toLowerCase.contains(field.value.toLowerCase()))
            .map(_.id.string)
          JSONQueryFilter(field.column,Some(Filter.NOTIN),ids.mkString(","))
        }
        case Some(Filter.FK_EQUALS) => {
          val id = getFieldLookup(field.column)
            .find(_.value == field.value)
            .map(_.id)
          JSONQueryFilter(field.column,Some(Filter.IN),id.map(_.string).getOrElse(""))  //fails with EQUALS when id = ""
        }
        case Some(Filter.FK_NOT) => {
          val id = getFieldLookup(field.column)
            .find(_.value == field.value)
            .map(_.id)
          JSONQueryFilter(field.column,Some(Filter.NOTIN),id.map(_.string).getOrElse("")) //fails with NOT when id = ""
        }
        case _ => field
      }
    }

    query.copy(filter = filters)

  }

  private def query():JSONQuery = {
    val fieldQueries = model.subProp(_.fieldQueries).get

    val sort = fieldQueries.filter(_.sort != Sort.IGNORE).sortBy(_.sortOrder.getOrElse(-1)).map(s => JSONSort(s.field.name, s.sort)).toList

    val filter = fieldQueries.filter(_.filterValue != "").map{ f =>
      JSONQueryFilter(f.field.name,Some(f.filterOperator),f.filterValue)
    }.toList

    logger.info(s"$filter")
    logger.info(s"$fieldQueries")

    JSONQuery(filter, sort, None, Some(services.clientSession.lang()))
  }

  def reloadRows(page:Int): Future[Unit] = {

    services.clientSession.loading.set(true)

    logger.info("reloading rows")
    logger.info("filterUpdateHandler "+filterUpdateHandler)

    val q = query().copy(paging = Some(JSONQueryPaging(ClientConf.pageLength, page)))
    val qEncoded = encodeFk(model.get.metadata.toSeq.flatMap(_.fields),q)

    val r = for {
      csv <- services.rest.csv(model.subProp(_.kind).get, services.clientSession.lang(), model.subProp(_.name).get, qEncoded)
      ids <- services.rest.ids(model.get.kind, services.clientSession.lang(), model.get.name, qEncoded)
    } yield {
      model.subProp(_.rows).set(csv.map(Row(_)))
      model.subProp(_.ids).set(IDsVM.fromIDs(ids))
      model.subProp(_.pages).set(Navigation.pageCount(ids.count))
      saveIds(ids, q)
      services.clientSession.loading.set(false)
    }

    r.recover{ _ => services.clientSession.loading.set(false) }

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

  def selected(row: => Row) = (e:Event) => {
    onSelect(model.get.fieldQueries.map(_.field).zip(row.data))
    model.subProp(_.selectedRow).set(Some(row))
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
  val downloadXLS = (e:Event) => {
    download("xlsx")
    e.preventDefault()
  }

  private def download(format:String) = {

    val kind = EntityKind(model.subProp(_.kind).get).entityOrForm
    val modelName =  model.subProp(_.name).get
    val exportFields = model.get.metadata.map(_.exportFields).getOrElse(Seq())
    val fields = model.get.metadata.map(_.fields).getOrElse(Seq())

    val queryWithFK = encodeFk(fields,query())


    val url = Routes.apiV1(
      s"/$kind/${services.clientSession.lang()}/$modelName/$format?fk=${ExportMode.RESOLVE_FK}&fields=${exportFields.mkString(",")}&q=${queryWithFK.asJson.toString()}".replaceAll("\n","")
    )
    logger.info(s"downloading: $url")
    dom.window.open(url)
  }


}

case class EntityTableView(model:ModelProperty[EntityTableModel], presenter:EntityTablePresenter, routes:Routes) extends View with Logging {
  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import ch.wsl.box.shared.utils.JSONUtils._




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
      metadata.toSeq.flatMap(_.fields).find(_.name == field).toSeq.flatMap(f => Filter.options(f))
    }

    Select(operator, options)(label,ClientConf.style.fullWidth,ClientConf.style.filterTableSelect)

  }

  def filterField(filterValue: Property[String], field:Option[JSONField], filterOperator:String):Modifier = {

    filterValue.listen(v => logger.info(s"Filter for ${field.map(_.name)} changed in: $v"))

    field.map(_.`type`) match {
      case Some(JSONFieldTypes.TIME) => DateTimeWidget.Time(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string)).edit()
      case Some(JSONFieldTypes.DATE) => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),true).edit()
      case Some(JSONFieldTypes.DATETIME) => ClientConf.filterPrecisionDatetime match{
        case JSONFieldTypes.DATE => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),true).edit()
        case _ => DateTimeWidget.DateTime(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),true).edit()
      }
      case Some(JSONFieldTypes.NUMBER) | Some(JSONFieldTypes.INTEGER) if field.flatMap(_.lookup).isEmpty && !Seq(Filter.BETWEEN, Filter.IN, Filter.NOTIN).contains(filterOperator) => {
        if(Try(filterValue.get.toDouble).toOption.isEmpty) filterValue.set("")
        NumberInput(filterValue)(ClientConf.style.fullWidth)
      }
      case _ => TextInput(filterValue)(ClientConf.style.fullWidth)
    }

  }

  override def getTemplate: scalatags.generic.Modifier[Element] = {

    val pagination = {

      div(ClientConf.style.boxNavigationLabel,
        Navigation.button(model.subProp(_.ids.currentPage).transform(_ != 1),() => presenter.reloadRows(1),Labels.navigation.first,_.Float.left()),
        Navigation.button(model.subProp(_.ids.currentPage).transform(_ != 1),() => presenter.reloadRows(model.subProp(_.ids.currentPage).get -1),Labels.navigation.previous,_.Float.left()),
        span(
          " " + Labels.navigation.page + " ",
          bind(model.subProp(_.ids.currentPage)),
          " " + Labels.navigation.of + " ",
          bind(model.subProp(_.pages)),
          " "
        ),
        Navigation.button(model.subModel(_.ids).subProp(_.isLastPage).transform(!_),() => presenter.reloadRows(model.subProp(_.pages).get),Labels.navigation.last,_.Float.right()),
        Navigation.button(model.subModel(_.ids).subProp(_.isLastPage).transform(!_),() => presenter.reloadRows(model.subProp(_.ids.currentPage).get + 1),Labels.navigation.next,_.Float.right()),
        div(Labels.navigation.recordFound," ",bind(model.subProp(_.ids.count)))
      )
    }

    produce(model.subProp(_.metadata)) { metadata =>
      div(
        div(BootstrapStyles.Float.left(),
          h3(ClientConf.style.noMargin, labelTitle(metadata))
        ),
        div(BootstrapStyles.Float.right(), ClientConf.style.navigatorArea,
          pagination.render
        ),
        div(BootstrapStyles.Visibility.clearfix),
        produceWithNested(model.subProp(_.access)) { (a, releaser) =>
          if (a.insert)
            div(BootstrapStyles.Float.left())(
              releaser(produce(model.subProp(_.name)) { m =>
                div(
                  button(ClientConf.style.boxButtonImportant, Navigate.click(Routes(model.subProp(_.kind).get, m).add()))(Labels.entities.`new`)
                ).render
              })
            ).render
          else Seq()
        },
        div(BootstrapStyles.Visibility.clearfix),
        div(id := "box-table", ClientConf.style.fullHeightMax,
          UdashTable(model.subSeq(_.rows))(

            headerFactory = Some(_ => {
              frag(
                tr(
                  td(ClientConf.style.smallCells)(),
                  metadata.toSeq.flatMap(_.tabularFields).map{ field =>
                    val fieldQuery:ReadableProperty[Option[FieldQuery]] = model.subProp(_.fieldQueries).transform(_.find(_.field.name == field))
                    val title: ReadableProperty[String] = fieldQuery.transform(_.flatMap(_.field.label).getOrElse(field))
                    val sort:ReadableProperty[String] = fieldQuery.transform(_.map(x => Labels(Sort.label(x.sort))).getOrElse(""))
                    val order:ReadableProperty[String] = fieldQuery.transform(_.flatMap(_.sortOrder).map(_.toString).getOrElse(""))

                    td(ClientConf.style.smallCells)(
                      a(
                        onclick :+= presenter.sort(fieldQuery),
                        span(bind(title), ClientConf.style.tableHeader), " ",
                        bind(sort), " ", bind(order)
                      )
                    ).render
                  }
                ),
                tr(
                  td(ClientConf.style.smallCells)(Labels.entity.filters),
                  metadata.toSeq.flatMap(_.tabularFields).map { field =>
                    val fieldQuery:Property[Option[FieldQuery]] = model.subProp(_.fieldQueries).bitransform(_.find(_.field.name == field)){ el =>
                      model.subProp(_.fieldQueries).get.map{old =>
                        if(old.field.name == field && el.isDefined) el.get else old
                      }
                    }
                    val filterValue:Property[String] = fieldQuery.bitransform(_.map(_.filterValue).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterValue = value)))
                    val operator:Property[String] = fieldQuery.bitransform(_.map(_.filterOperator).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterOperator = value)))
                    val jsonField = metadata.flatMap(_.fields.find(_.name == field))

                    td(ClientConf.style.smallCells)(
                      filterOptions(metadata,field,operator),
                      produce(operator) { op =>
                        div(position.relative, filterField(filterValue, jsonField, op)).render
                      }
                    ).render

                  }
                )
              ).render
            }),
            rowFactory = (el, nested) => {
              val key = presenter.ids(el.get)
              val hasKey = metadata.exists(_.keys.nonEmpty)
              val selected = model.subProp(_.selectedRow).transform(_.exists(_ == el.get))

              def show = a(
                cls := "primary action",
                onclick :+= presenter.show(el.get)
              )(Labels.entity.show)

              def edit = a(
                cls := "primary action",
                onclick :+= presenter.edit(el.get)
              )(Labels.entity.edit)

              def delete = a(
                cls := "danger action",
                onclick :+= presenter.delete(el.get)
              )(Labels.entity.delete)

              def noAction = p(color := "grey")(Labels.entity.no_action)

              tr((`class` := "info").attrIf(selected), ClientConf.style.rowStyle, onclick :+= presenter.selected(el.get),
                td(ClientConf.style.smallCells)(
                  (hasKey, model.get.access.update, model.get.access.delete) match {
                    case (false, _, _) => noAction
                    case (true, false, false) => show
                    case (true, false, true) => Seq(show, span(" "), delete)
                    case (true, true, true) => Seq(edit, span(" "), delete)
                    case (true, true, false) => Seq(edit)
                  }
                ),

                for {(f, i) <- metadata.toSeq.flatMap(_.tabularFields).zipWithIndex} yield {

                  val value = el.get.data.lift(i).getOrElse("")
                  metadata.flatMap(_.fields.find(_.name == f)) match {
                    case Some(field) => td(ClientConf.style.smallCells)(TableFieldsRenderer(
                      value,
                      field,
                      key,
                      routes
                    )).render
                    case None => td().render
                  }

                }

              ).render
            }
          ).render,

          button(`type` := "button", onclick :+= presenter.downloadCSV, ClientConf.style.boxButton, Labels.entity.csv),
          button(`type` := "button", onclick :+= presenter.downloadXLS, ClientConf.style.boxButton, Labels.entity.xls),
          showIf(model.subProp(_.fieldQueries).transform(_.size == 0)) {
            p("loading...").render
          },
          br, br
        ),
        Debug(model)
      ).render
    }
  }


}
