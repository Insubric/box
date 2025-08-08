package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.views.components.widget.InputWidgetFactory.Input
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{EntityKind, Filter, JSONField, JSONQuery, JSONQueryFilter, JSONQueryPaging, JSONSort, Sort, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.udash.bindings.modifiers.Binding
import io.circe.generic.auto._
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.utils.{BootstrapTags, UdashIcons}
import io.udash.css.CssView._
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import scalacss.ScalatagsCss._

import scala.concurrent.{ExecutionContext, Future}


object QueryBuilderWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminQueryBuilder


  override def create(params: WidgetParams): Widget = QueryBuilderWidgetImpl(params)

  case class QueryBuilderWidgetImpl(params: WidgetParams) extends Widget with Logging {

    override def field: JSONField = params.field


    /**
     * Used to provide the user the human-readable representation of the data, mainly used for lookups
     *
     * @param json data as stored in the database
     * @param ex
     * @return user readable data
     */
    override def toUserReadableData(json: Json)(implicit ex: ExecutionContext): Future[Json] = Future.successful(json.as[JSONQuery].map(_.asString).getOrElse("").asJson)

    override protected def show(nested: Binding.NestedInterceptor): JsDom.all.Modifier = edit(nested)

    override protected def edit(nested: Binding.NestedInterceptor): JsDom.all.Modifier = {

      val query:Property[Option[JSONQuery]] = params.prop.bitransform(_.as[JSONQuery].toOption)(_.asJson)

      def getQuery = query.get match {
        case Some(value) => value
        case None => JSONQuery.empty
      }

      val columns:SeqProperty[JSONField] = SeqProperty(Seq())

      import ch.wsl.box.client.Context._
      import Implicits.executionContext

      params.fieldParams.foreach{x =>
        x.listen({ p =>
          p.getOpt("entity") match {
            case Some(entity) => services.rest.metadata(EntityKind.ENTITY.kind,services.clientSession.lang(),entity,false).foreach{ m =>
              columns.set(m.fields)
            }
            case None => ()
          }
        },true)

      }


      def firstColumn = columns.get.headOption.map(_.name).getOrElse("")

      def addFilter():Unit = {

        query.set(Some(
          getQuery.copy(filter = getQuery.filter ++ Seq(JSONQueryFilter.withValue(firstColumn,Some(Filter.EQUALS),"")))
        ))
      }

      def removeFilter(i:ReadableProperty[Int])():Unit = {
        query.set(Some(
          getQuery.copy(filter = getQuery.filter.zipWithIndex.filterNot(_._2 == i.get).map(_._1))
        ))
      }


      def addOrder():Unit = {
        query.set(Some(
          getQuery.copy(sort = getQuery.sort ++ Seq(JSONSort(firstColumn,Sort.ASC)))
        ))
      }

      def removeOrder(i:ReadableProperty[Int])():Unit = {
        query.set(Some(
          getQuery.copy(sort = getQuery.sort.zipWithIndex.filterNot(_._2 == i.get).map(_._1))
        ))
      }

      def orderSwitch(_i:ReadableProperty[Int],offset:Int)():Unit = {

        val current = _i.get
        val s = getQuery.sort

        val newSort = s.zipWithIndex.map {
          case (elem,i)  if i == current => s(current + offset)
          case (elem,i)  if i == current + offset => s(current)
          case (elem,_) => elem
        }

        query.set(Some(
          getQuery.copy(sort = newSort)
        ))
      }



      def buttonArea(
                      add:Option[() => Unit] = None,
                      remove:Option[() => Unit] = None,
                      first:ReadableProperty[Boolean] = Property(false),
                      up: Option[() => Unit] = None,
                      last:ReadableProperty[Boolean] = Property(false),
                      down: Option[() => Unit] = None,
                      nested: NestedInterceptor = Binding.NestedInterceptor.Identity
                    ) = {
        div(
          style := "display:flex; justify-content: flex-end; width: 200px",
          add.map(a => button(Icons.plus,ClientConf.style.boxButton, onclick :+= ((e:Event) => a()))),
          remove.map(a => button(Icons.x,ClientConf.style.boxButton, onclick :+= ((e:Event) => a()))),
          up.map(a =>
            nested(showIf(first.transform(!_)) {
              button(i(UdashIcons.FontAwesome.Solid.caretUp),ClientConf.style.boxButton, onclick :+= ((e:Event) => a())).render
            })
          ),
          down.map(a =>
            nested(showIf(last.transform(!_)) {
              button(i(UdashIcons.FontAwesome.Solid.caretDown),ClientConf.style.boxButton, onclick :+= ((e:Event) => a())).render
            })
          ),
        )
      }

      def fieldLabel(f:JSONField):Modifier = f.title

      def filterRow(row:Property[JSONQueryFilter],i:ReadableProperty[Int],interceptor: NestedInterceptor) = {

        val column = row.bitransform(x => columns.get.find(_.name == x.column).getOrElse(JSONField.string(x.column)))(x => row.get.copy(column = x.name))
        val operator = row.bitransform(_.operator.getOrElse(Filter.EQUALS))(x => row.get.copy(operator = Some(x)))
        val value = row.bitransform(_.value.getOrElse(""))(x => row.get.copy(value = if(x.isEmpty) None else Some(x)))
        val fieldValue = row.bitransform(x => columns.get.find(c => x.fieldValue.contains(c.name))){x => row.get.copy(fieldValue = x.map(_.name)) }
        val filterKind = Property("Field")
        filterKind.listen( fk => if(fk == "Value") fieldValue.set(None))
        fieldValue.listen( fk => if(fk.isEmpty) filterKind.setInitValue("Value"),true)

        div(style := "display: flex; align-items: center;",
          Select(column,columns)(fieldLabel),
          interceptor(produce(column) { f =>
            Select(operator, Filter.options(f,false).toSeqProperty)(Select.defaultLabel).render
          }),
          RadioButtons(filterKind,Seq("Value","Field").toSeqProperty)(els => div(style := "display:flex; justify-content: space-evenly; min-width: 200px",els.map {
            case (i: Input, l: String) => label(style := "display: flex; margin-right: 10px; margin-bottom: 0", BootstrapTags.dataLabel := l)(i, span(marginLeft := 5,l))
          }).render, width := 30),
          showIf(filterKind.transform(_ == "Value")){
            TextInput(value)(placeholder := "Value").render
          },
          showIf(filterKind.transform(_ == "Field")){
            Select.optional(fieldValue,columns,"---")(fieldLabel,placeholder := "Field").render
          },
          buttonArea(remove = Some(removeFilter(i)))
        )
      }

      def orderRow(row:Property[JSONSort],i:ReadableProperty[Int]) = {

        val column = row.bitransform(x => columns.get.find(_.name == x.column).getOrElse(JSONField.string(x.column)))(x => row.get.copy(column = x.name))
        val order = row.bitransform(_.order)(x => row.get.copy(order = x))

        val first = i.transform(_ == 0)
        val last = i.combine(query)((i,q) => q.forall( q => i == q.sort.length -1))

        div(style := "display: flex; align-items: center;",
          Select(column, columns)(fieldLabel),
          Select(order, Seq(Sort.ASC, Sort.DESC).toSeqProperty)(Select.defaultLabel),
          buttonArea(remove = Some(removeOrder(i)), first = first, last = last, up = Some(orderSwitch(i,-1)), down = Some(orderSwitch(i,1)))
        )
      }

      val filters = query.bitransformToSeq(_.toList.flatMap(_.filter))(x => query.get.map(_.copy(filter = x.toList)) )
      val orders = query.bitransformToSeq(_.toList.flatMap(_.sort))(x => query.get.map(_.copy(sort = x.toList) ))
      val limit = query.bitransform(_.flatMap(_.paging.map(_.pageLength.toString)).getOrElse("")){ l =>
        val lim = l match {
          case "" => None
          case s: String => l.toIntOption
        }
        query.get.map(_.copy(paging = lim.map(l => JSONQueryPaging(l))))
      }


      div(ClientConf.style.queryBuilderContainer,
        div(
          h4("Filter"),
          repeatWithIndex(filters){ (filter,i,interceptor) =>
            filterRow(filter,i,interceptor).render
          },
          buttonArea(add = Some(addFilter))
        ),
        div(
          h4("Order"),
          repeatWithIndex(orders){ (order,i,interceptor) =>
            orderRow(order,i).render
          },
          buttonArea(add = Some(addOrder))
        ),
        div(
          h4("Limit"),
          div(style := "display: flex; align-items: center;",
            NumberInput(limit)(float.none)
          )
        )
      )

    }
  }
}