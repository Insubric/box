package ch.wsl.box.client.views.components.table

import ch.wsl.box.client.services.{ClientConf, Labels, UI}
import ch.wsl.box.client.views.FieldQuery
import ch.wsl.box.model.shared.{ JSONField, JSONFieldTypes, JSONLookups, JSONMetadata}

import io.udash._
import io.udash.properties.single.{Property, ReadableProperty}
import org.scalajs.dom.{Event, HTMLElement, MutationObserver, MutationObserverInit, document}



class FilterEveryField(val fieldQueries:Property[Seq[FieldQuery]], val lookups:ReadableProperty[Seq[JSONLookups]]) extends FilterBar {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  def render(columns:Seq[JSONField],metadata:JSONMetadata):HTMLElement = tr(
    td(ClientConf.style.smallCells, colspan := 2)(Labels.entity.filters),
    columns.filterNot(_.`type` == JSONFieldTypes.GEOMETRY).map { _field =>

      val  (filterValue,operator) = filterPropsField(_field)

      td(ClientConf.style.smallCells)(
        filterOptions(metadata, _field.name, operator)(ClientConf.style.fullWidth,ClientConf.style.filterTableSelect),
        produceWithNested(operator) { (op, nested) =>
          div(position.relative, filterField(filterValue, _field, op, nested)(ClientConf.style.fullWidth)).render
        }
      ).render

    }
  ).render
}
