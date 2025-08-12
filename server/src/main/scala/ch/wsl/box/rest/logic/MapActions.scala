package ch.wsl.box.rest.logic

import ch.wsl.box.model.shared.GeoJson.{Feature, FeatureCollection}
import ch.wsl.box.model.shared.{JSONDiff, JSONDiffField, JSONDiffModel}
import ch.wsl.box.model.shared.geo.DbVectorProperties
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

object MapActions {
  def save(geom:Feature)(implicit ex:ExecutionContext,services: Services) = {
    geom.properties.flatMap(_.asJson.as[DbVectorProperties].toOption) match {
      case Some(value) => Registry().actions(value.entity).updateDiff(
        JSONDiff(Seq(JSONDiffModel(value.entity,Some(value.id),Seq(JSONDiffField(value.field,None,Some(geom.geometry.asJson))))))
      )
      case None => DBIO.successful(None)
    }
  }
}
