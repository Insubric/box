package ch.wsl.box.client.utils

import ch.wsl.box.model.shared.{Filter, JSONField, JSONLookup, JSONQuery, JSONQueryFilter}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

object FKEncoder {
  def apply(fields:Seq[JSONField],query:JSONQuery):JSONQuery = {

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

}
