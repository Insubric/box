package ch.wsl.box.client.views.components.table

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.views.components.{ModalDef, ModalStack}
import ch.wsl.box.model.shared.{Filter, JSONField, JSONLookups, JSONMetadata, JSONQueryFilter, JSONSort, Sort}
import org.scalajs.dom.HTMLElement
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom.{Event, HTMLElement, MutationObserver, MutationObserverInit, document}

import java.util.UUID


class FilterBarDyn(val filters:Property[Seq[JSONQueryFilter]],sorts:Property[Seq[JSONSort]], val lookups:ReadableProperty[Seq[JSONLookups]]) extends FilterBar {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  //val filterFields:Property[Seq[FieldQuery]] = fieldQueries.bitransform(_.filter(_.filterValue.nonEmpty))(s => fieldQueries.get.map(f => s.find(_.field.name == f.field.name).getOrElse(f)))
//  val _filterFields:Property[Seq[JSONField]] = Property(Seq())
//  val _filterFields:SeqProperty[JSONField] = SeqProperty(Seq())
//
//  val _sortFields:SeqProperty[JSONField] = SeqProperty(Seq())

//  fieldQueries.listen({x =>
//    val newFilters:Seq[JSONField] = x.filter(y => y.filterValue.nonEmpty  && !_filterFields.get.map(_.name).contains(y.field.name)).map(_.field)
//    _filterFields.append(newFilters:_*)
//
//    val newSorts:Seq[JSONField] = x.filter(y => y.sort.nonEmpty  && !_sortFields.get.map(_.name).contains(y.field.name)).map(_.field)
//    _sortFields.append(newSorts:_*)
//  },initUpdate = true)

//  def _render(columns: Seq[JSONField], metadata: JSONMetadata): HTMLElement = {
//
//    div( ClientConf.style.filterDynBar,
//      //div(pre(bind(fieldQueries.transform(_.map(x => s"Field: ${x.field.name}, ${x.filterValue} , Sort: ${x.sort} ${x.sortOrder}").mkString("\n"))))),
//      div(ClientConf.style.filterBlockTitle,Icons.filter,"Filters"),
//      repeat(_filterFields) { (fieldProp) =>
//        div(ClientConf.style.filterBlock,
//          Select(fieldProp,fieldQueries.transformToSeq(_.map(_.field)))(_.title),
//          produce(fieldProp) { (field) =>
//
//            val  (filterValue,operator) = filterPropsField(field)
//
//            div(
//              display.flex,alignItems.center,
//              filterOptions(metadata, field.name, operator)(),
//              produceWithNested(operator) { (op, nested) =>
//                div(filterField(filterValue, field, op, nested)()).render
//              },
//              button(Icons.x,ClientConf.style.boxButtonIconMini,onclick :+= ((e:Event) => {
//                filterValue.set("")
//                _filterFields.remove(field)
//              }))
//            ).render
//          },
//
//        ).render
//      },
//
//      button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
//        fieldQueries.get.find(_.filterValue.isEmpty).map(x => _filterFields.append(x.field))
//      })),
//      div(Icons.asc,"Sort",ClientConf.style.filterBlockTitle),
//      repeatWithIndex(_sortFields) { case (fieldProp,i,nested) =>
//        div(ClientConf.style.filterBlock,
//          Select(fieldProp,fieldQueries.transformToSeq(_.map(_.field)))(_.title),
//          produce(fieldProp.combine(i)((x,y) => (x,y))) { case (field, i) =>
//
//            val sortProp = fieldQueries.bitransform(_.find(_.field == field).map(_.sort))(x => fieldQueries.get.map { fq => if (fq.field == field) {
//              x match {
//                case Some(value) => fq.copy(sort = value, sortOrder = Some(i+1))
//                case None => fq.copy(sort = "", sortOrder = None)
//              }
//
//            } else fq })
//            div(
//              display.flex,alignItems.center,
//              Select.optional(sortProp, SeqProperty(Seq(Sort.ASC,Sort.DESC)),"---")(x => x.toUpperCase),
//              button(Icons.x,ClientConf.style.boxButtonIconMini,onclick :+= ((e:Event) => {
//                sortProp.set(None)
//                _sortFields.remove(field)
//                fieldQueries.set(fieldQueries.get.map{fq =>
//                  if(fq.sortOrder.exists(_ > i+1)) {
//                    fq.copy(sortOrder = fq.sortOrder.map(_ - 1))
//                  } else fq
//                })
//
//              }))
//            ).render
//          }
//
//        ).render
//      },
//      button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
//        fieldQueries.get.find(_.sort.isEmpty).map(x => _sortFields.append(x.field))
//      })),
//    ).render
//  }

  val modalId = UUID.randomUUID()


  def sortPopup(columns: Seq[JSONField],existing:Option[JSONSort] = None):ModalDef = {
    val sortProp = Property(existing.getOrElse(JSONSort(columns.head.name,Sort.ASC)))
    val sortDirection = sortProp.bitransform(_.order)(x => sortProp.get.copy(order = x))
    val sortField = sortProp.bitransform(x => columns.find(_.name == x.column).get)(x => sortProp.get.copy(column = x.name))

    ModalDef(
      modalId,
      headerFactory = None,
      bodyFactory = Some(n => {
        div(
          display.flex,alignItems.center,justifyContent.spaceAround,flexDirection.column,height := 200.px,
          Select(sortField,columns.toSeqProperty)(_.title),
          Select(sortDirection, SeqProperty(Seq(Sort.ASC,Sort.DESC)))(x => x.toUpperCase),
        ).render
      }),
      footerFactory = Some(_ => {
        div(
          button(onclick :+= ((e: Event) => {
            ModalStack.mainStack.pop(modalId)
            e.preventDefault()
          }), Labels.popup.close, ClientConf.style.boxButton),
          button(onclick :+= ((e: Event) => {
            ModalStack.mainStack.pop(modalId)

            val newSort = existing  match {
              case Some(value) => sorts.get.map(s => if(s == value) sortProp.get else s)
              case None => sorts.get.filterNot(_.column == sortProp.get.column) ++ Seq(sortProp.get)
            }

            sorts.set(newSort)

            e.preventDefault()
          }), Labels.popup.sort, ClientConf.style.boxButtonImportant),
          existing.map{ ex =>
            button(onclick :+= ((e: Event) => {
              ModalStack.mainStack.pop(modalId)
              sorts.set(sorts.get.filterNot(_.column == ex.column))
              e.preventDefault()
            }), Labels.popup.remove, ClientConf.style.boxButtonDanger)
          }

        ).render
      }),
      size = Some(BootstrapStyles.Size.Small),
      onClose = None,
      onOpen = None
    )
  }
  def filterPopup(columns: Seq[JSONField],existing:Option[JSONQueryFilter] = None):ModalDef = {
    val filterProp:Property[JSONQueryFilter] = Property(existing.getOrElse(JSONQueryFilter(columns.head.name,None,None,None)))
    val _filterField = filterProp.bitransform(x => columns.find(_.name == x.column).get)(x => filterProp.get.copy(column = x.name))


    ModalDef(
      modalId,
      headerFactory = None,
      bodyFactory = Some(n => {
        div(
          display.flex,alignItems.center,justifyContent.spaceAround,flexDirection.column,height := 200.px,
          Select(_filterField,columns.toSeqProperty)(_.title),
          n(produce(_filterField){ f =>

            val filterValue: Property[String] = filterProp.bitransform[String](_.value.getOrElse(""))(x => { if(x.isEmpty) filterProp.get.copy(value = None) else filterProp.get.copy(value = Some(x)) })

            val operator: Property[String] = filterProp.bitransform(_.operator.getOrElse(""))(value => filterProp.get.copy(operator = Some(value)))

            frag(
              filterOptions(f, operator)(ClientConf.style.fullWidth).render,
              div(ClientConf.style.inputDefaultWidth,n(produceWithNested(operator) { (op, nested) =>
                div(filterField(filterValue, f, op, nested)(ClientConf.style.fullWidth)).render
              }))
            ).render
          })

        ).render
      }),
      footerFactory = Some(_ => {
        div(
          button(onclick :+= ((e: Event) => {
            ModalStack.mainStack.pop(modalId)
            e.preventDefault()
          }), Labels.popup.close, ClientConf.style.boxButton),
          button(onclick :+= ((e: Event) => {
            ModalStack.mainStack.pop(modalId)

            val newFilter = existing  match {
              case Some(value) => filters.get.map(s => if(s == value) filterProp.get else s)
              case None => filters.get.filterNot(_.column == filterProp.get.column) ++ Seq(filterProp.get)
            }

            filters.set(newFilter)

            e.preventDefault()
          }), Labels.popup.filter, ClientConf.style.boxButtonImportant),
          existing.map{ ex =>
            button(onclick :+= ((e: Event) => {
              ModalStack.mainStack.pop(modalId)
              filters.set(filters.get.filterNot(_.column == ex.column))
              e.preventDefault()
            }), Labels.popup.remove, ClientConf.style.boxButtonDanger)
          }

        ).render
      }),
      size = Some(BootstrapStyles.Size.Small),
      onClose = None,
      onOpen = None
    )
  }

  override def render(columns: Seq[JSONField], metadata: JSONMetadata,nested:Binding.NestedInterceptor): HTMLElement = {

    div( ClientConf.style.filterDynBar,
      //div(pre(bind(fieldQueries.transform(_.map(x => s"Field: ${x.field.name}, ${x.filterValue} , Sort: ${x.sort} ${x.sortOrder}").mkString("\n"))))),
      div(ClientConf.style.flexDyn,
        div(display.flex,alignItems.center,
          div(ClientConf.style.filterBlockTitle,Icons.filter,"Filters"),
          button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
            ModalStack.mainStack.push(filterPopup(columns,None))
          }))
        ),

        nested(produce(filters)( _.map{ filter =>
          val name = columns.find(_.name == filter.column ).map(_.title).getOrElse(filter.column)
          a(ClientConf.style.chipLink,filter.asString(name), onclick :+= ((e:Event) => ModalStack.mainStack.push(filterPopup(columns,Some(filter))))).render
        }))
      ),

      div(ClientConf.style.flexDyn,
        div(display.flex,alignItems.center,
          div(Icons.asc,"Sort",ClientConf.style.filterBlockTitle),
          button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
            ModalStack.mainStack.push(sortPopup(columns))
          }))
        ),
        nested(produce(sorts)( _.map{ sort =>
          val name = columns.find(_.name == sort.column ).map(_.title).getOrElse(sort.column)
          a(ClientConf.style.chipLink,sort.asString(name), onclick :+= ((e:Event) => ModalStack.mainStack.push(sortPopup(columns,Some(sort))))).render
        }))
      )

    ).render
  }
}
