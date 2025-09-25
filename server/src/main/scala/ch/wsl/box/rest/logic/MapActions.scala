package ch.wsl.box.rest.logic

import ch.wsl.box.model.shared.GeoJson.{Feature, FeatureCollection}
import ch.wsl.box.model.shared.{JSONDiff, JSONDiffField, JSONDiffModel}
import ch.wsl.box.model.shared.geo.DbVectorProperties
import ch.wsl.box.rest.io.geotools.GeoJsonConverter
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import scribe.Logging
import slick.dbio.DBIO
import ch.wsl.box.rest.utils.GeoJsonSupport._

import scala.concurrent.ExecutionContext

object MapActions extends Logging {
  def save(geom:Feature)(implicit ex:ExecutionContext,services: Services) = {
    geom.properties.map(_.asJson.as[DbVectorProperties]) match {
      case Some(Right(value)) => {
        Registry().actions(value.entity).updateDiff(
          JSONDiff(Seq(JSONDiffModel(value.entity,Some(value.id),Seq(JSONDiffField(value.field,None,Some(GeoJsonConverter.toJTS(geom.geometry).asJson))))))
        )
      }
      case Some(Left(e)) => {
        logger.debug(s"properties not decoded correctly with ${e.message}")
        DBIO.successful(None)
      }
      case None => DBIO.successful(None)
    }
  }
}
