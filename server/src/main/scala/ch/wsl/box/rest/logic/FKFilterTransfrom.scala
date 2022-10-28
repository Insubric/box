package ch.wsl.box.rest.logic

import ch.wsl.box.model.shared.{Filter, JSONField, JSONLookup, JSONLookups, JSONLookupsFieldRequest, JSONMetadata, JSONQuery, JSONQueryFilter}
import ch.wsl.box.model.shared.JSONQueryFilter.WHERE
import ch.wsl.box.rest.runtime.RegistryInstance
import io.circe.Json
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

import scala.concurrent.ExecutionContext

case class PreFiltered(filters: Seq[JSONQueryFilter])

class FKFilterTransfrom(registry:RegistryInstance)(implicit ec:ExecutionContext, services: Services) {

  def singleLookup(metadata:JSONMetadata)(field: JSONLookupsFieldRequest):DBIO[JSONLookups] = {
    val jsonField = metadata.fields.find(_.name == field.fieldName).get
    val lookup = jsonField.lookup.get
    val remoteLabels = lookup.map.textProperty.split(",").toSeq
    val query = JSONQuery.filterWith(WHERE.in(lookup.map.valueProperty,field.values.map(_.string)))
    registry.actions(lookup.lookupEntity).findSimple(query).map{ rows =>
      JSONLookups(
        field.fieldName,
        rows.map(row => JSONLookup(row.js(lookup.map.valueProperty),remoteLabels.map(row.get)))
      )
    }
  }

  def fkFilterFor(filter:JSONQueryFilter,field:JSONField):DBIO[Option[JSONQueryFilter]] = {
    val jsonActions = registry.actions(field.lookup.get.lookupEntity)

    def toParentValues(rows:Seq[Json]):Seq[String] = rows.flatMap(_.getOpt(field.lookup.get.map.valueProperty)).distinct

    def transfrom(remoteFilter: String => JSONQueryFilter,localFilter: Seq[String] => JSONQueryFilter) = {
      val remoteLabels = field.lookup.get.map.textProperty.split(",").toSeq
      DBIO.sequence(remoteLabels.map(l =>
        jsonActions.findSimple(JSONQuery.filterWith(remoteFilter(l)).limit(9999999)))
      )
        .map(_.flatten)
        .map(toParentValues).map{ v =>
        Some(localFilter(v))
      }
    }

    def like:DBIO[Option[JSONQueryFilter]] = transfrom(WHERE.like(_,filter.value),WHERE.in(filter.column,_))
    def equals:DBIO[Option[JSONQueryFilter]] = transfrom(WHERE.eq(_,filter.value),WHERE.in(filter.column,_))
    def dislike:DBIO[Option[JSONQueryFilter]] = transfrom(WHERE.like(_,filter.value),WHERE.notIn(filter.column,_))
    def not:DBIO[Option[JSONQueryFilter]] = transfrom(WHERE.eq(_,filter.value),WHERE.notIn(filter.column,_))

    filter.operator match {
      case Some(Filter.FK_LIKE) => like
      case Some(Filter.FK_EQUALS) => equals
      case Some(Filter.FK_DISLIKE) => dislike
      case Some(Filter.FK_NOT) => not
      case _ => DBIO.successful(None)
    }
  }

  def preFilter(m:JSONMetadata,filters: Seq[JSONQueryFilter]):DBIO[PreFiltered] = {

    val (fkFilters,stdFilters) = filters.partition(_.fkFilter)

    for {
      trasformedFilters <- DBIO.sequence(fkFilters.flatMap{ filter =>
        m.fields.find(f => f.name == filter.column && f.lookup.isDefined).map{ field =>
          fkFilterFor(filter,field)
        }
      }).map(_.flatten)
    } yield PreFiltered(trasformedFilters ++ stdFilters)

  }

}
