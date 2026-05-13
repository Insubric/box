package ch.wsl.box.client.views.components.table

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.views.FieldQuery
import ch.wsl.box.model.shared.{JSONField, JSONLookups, JSONMetadata, JSONQueryFilter, Sort}
import io.udash.properties.single.{Property, ReadableProperty}
import org.scalajs.dom.HTMLElement
import io.udash._
import io.udash.properties.single.{Property, ReadableProperty}
import org.scalajs.dom.{Event, HTMLElement, MutationObserver, MutationObserverInit, document}


class FilterBarDyn(val fieldQueries:Property[Seq[FieldQuery]], val lookups:ReadableProperty[Seq[JSONLookups]]) extends FilterBar {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  //val filterFields:Property[Seq[FieldQuery]] = fieldQueries.bitransform(_.filter(_.filterValue.nonEmpty))(s => fieldQueries.get.map(f => s.find(_.field.name == f.field.name).getOrElse(f)))
//  val _filterFields:Property[Seq[JSONField]] = Property(Seq())
  val _filterFields:SeqProperty[JSONField] = SeqProperty(Seq())

  val _sortFields:SeqProperty[JSONField] = SeqProperty(Seq())

  fieldQueries.listen({x =>
    val newFilters:Seq[JSONField] = x.filter(y => y.filterValue.nonEmpty  && !_filterFields.get.map(_.name).contains(y.field.name)).map(_.field)
    _filterFields.append(newFilters:_*)

    val newSorts:Seq[JSONField] = x.filter(y => y.sort.nonEmpty  && !_sortFields.get.map(_.name).contains(y.field.name)).map(_.field)
    _sortFields.append(newSorts:_*)
  },initUpdate = true)

  override def render(columns: Seq[JSONField], metadata: JSONMetadata): HTMLElement = {





    div( display.flex,alignItems.center,
      div(Icons.filter,"Filters",fontWeight.bold,marginLeft := 15.px),
      repeat(_filterFields) { (fieldProp) =>
        div(ClientConf.style.filterBlock,
          Select(fieldProp,fieldQueries.transformToSeq(_.map(_.field)))(_.title),
          produce(fieldProp) { (field) =>

            val  (filterValue,operator) = filterPropsField(field)

            div(
              display.flex,alignItems.center,
              filterOptions(metadata, field.name, operator)(),
              produceWithNested(operator) { (op, nested) =>
                div(filterField(filterValue, field, op, nested)()).render
              },
              button(Icons.x,ClientConf.style.boxButtonIconMini,onclick :+= ((e:Event) => {
                filterValue.set("")
                _filterFields.remove(field)
              }))
            ).render
          },

        ).render
      },

      button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
        fieldQueries.get.find(_.filterValue.isEmpty).map(x => _filterFields.append(x.field))
      })),
      div(Icons.asc,"Sort",fontWeight.bold,marginLeft := 15.px),
      repeatWithIndex(_sortFields) { case (fieldProp,i,nested) =>
        div(ClientConf.style.filterBlock,
          Select(fieldProp,fieldQueries.transformToSeq(_.map(_.field)))(_.title),
          produce(fieldProp.combine(i)((x,y) => (x,y))) { case (field, i) =>

            val sortProp = fieldQueries.bitransform(_.find(_.field == field).map(_.sort))(x => fieldQueries.get.map { fq => if (fq.field == field) fq.copy(sort = x.getOrElse(""), sortOrder = Some(i+1)) else fq })
            div(
              display.flex,alignItems.center,
              Select.optional(sortProp, SeqProperty(Seq(Sort.ASC,Sort.DESC)),"---")(x => x.toUpperCase),
              button(Icons.x,ClientConf.style.boxButtonIconMini,onclick :+= ((e:Event) => {
                sortProp.set(None)
                _sortFields.remove(field)
              }))
            ).render
          }

        ).render
      },
      button(Icons.plus,ClientConf.style.boxButton,onclick :+= ((e:Event) => {
        fieldQueries.get.find(_.sort.isEmpty).map(x => _sortFields.append(x.field))
      })),
    ).render
  }
}
