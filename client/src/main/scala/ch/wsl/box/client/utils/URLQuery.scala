package ch.wsl.box.client.utils

import ch.wsl.box.model.shared.{Filter, JSONMetadata, JSONQuery}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.parser.parse
import io.circe.generic.auto._
import org.scalajs.dom.window
import scribe.Logging

import scala.scalajs.js.URIUtils

object URLQuery extends Logging {
  def fromQueryParameters(query:Option[String]):Option[JSONQuery] = query.flatMap{ q =>

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
          case Right(query) => Some(query)
        }
      }
    }
  }

  def fromState(query: Option[String]): Option[JSONQuery] = query.flatMap { q =>

    parse(window.atob(q)) match {
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
          case Right(query) => Some(query)
        }
      }
    }
  }

}
