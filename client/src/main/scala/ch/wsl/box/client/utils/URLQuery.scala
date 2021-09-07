package ch.wsl.box.client.utils

import ch.wsl.box.model.shared.{Filter, JSONMetadata, JSONQuery}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.parser.parse
import io.circe.generic.auto._
import scribe.Logging

import scala.scalajs.js.URIUtils

object URLQuery extends Logging {
  def apply(query:Option[String],metadata:JSONMetadata):Option[JSONQuery] = query.flatMap{ q =>

    parse(URIUtils.decodeURIComponent(q)) match {
      case Left(value) => {
        logger.warn(s"Failed to parse query ${value.message} \n $q")
        None
      }
      case Right(value) => {
        value.as[JSONQuery] match {
          case Left(value) => {
            logger.warn(s"Failed to parse query ${value.message}")
            None
          }
          case Right(query) => {
            val filters = query.filter.map{ fil =>
              metadata.fields.find(_.name == fil.column).flatMap(_.lookup) match {
                case Some(lookup) => {
                  if(fil.operator.exists(_.startsWith("FK"))) {
                    fil
                  } else {
                    lookup.lookup.find(_.id.string == fil.value) match {
                      case Some(lk) => fil.copy(operator = Some(Filter.FK_LIKE),value = lk.value)
                      case None => fil
                    }
                  }
                }
                case None => fil
              }
            }
            Some(query.copy(filter = filters))
          }
        }
      }
    }
  }
}
