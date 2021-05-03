package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.model.shared.JSONField
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol.{baseTileMod, layerMod, proj4Mod, projectionMod, sourceMod}

import scala.scalajs.js

/*
{
"features": {
    "point":  true,
    "line": false,
    "polygon": true
},
"multiGeometry": false,
"projection": {
    "name": "EPSG:21781",
    "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.4,15.1,405.3,0,0,0,0 +units=m +no_defs",
    "extent": [485071.54,75346.36,828515.78,299941.84]
}
}
*/
case class MapParamsFeatures(
                              point: Boolean,
                              multiPoint: Boolean,
                              line: Boolean,
                              multiLine:Boolean,
                              polygon: Boolean,
                              multiPolygon: Boolean,
                              geometryCollection: Boolean
                            )

case class MapParamsProjection(
                                name:String,
                                proj:String,
                                extent: Seq[Double],
                                unit: String
                              )

case class MapParamsLayers(
                            name: String,
                            capabilitiesUrl: String,
                            layerId:String,
                            time:Option[String]
                          )

case class MapParams(
                      features: MapParamsFeatures,
                      defaultProjection: String,
                      projections: Seq[MapParamsProjection],
                      baseLayers: Option[Seq[MapParamsLayers]]
                    )

trait MapWidget extends Logging {

  def field:JSONField

  val defaultParams = MapParams(
    features = MapParamsFeatures(true,true,true,true,true,true,true),
    defaultProjection = "EPSG:3857",
    projections = Seq(MapParamsProjection(
      name = "EPSG:3857",
      proj = "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs",
      extent = Seq(-20026376.39, -20048966.10, 20026376.39, 20048966.10),
      unit = "m"
    )),
    None
  )

  val wgs84 = MapParamsProjection(
    name = "EPSG:4326",
    proj = "+proj=longlat +datum=WGS84 +no_defs",
    extent = Seq(-180, -90, 180, 90),
    unit = "m"
  )

  val openStreetMapLayer = new layerMod.Tile(baseTileMod.Options().setSource(new sourceMod.OSM()))

  val jsonOption:Json = ClientConf.mapOptions.deepMerge(field.params.getOrElse(JsonObject().asJson))
  val options:MapParams = jsonOption.as[MapParams] match {
    case Right(value) => value
    case Left(error) => {
      logger.warn(s"Using default params - ${error} on json: $jsonOption")
      defaultParams
    }
  }

  logger.info(s"$options")

  typings.proj4.mod.^.asInstanceOf[js.Dynamic].default.defs(
    wgs84.name,
    wgs84.proj
  )

  options.projections.map { projection =>
    //typings error need to map it manually
    typings.proj4.mod.^.asInstanceOf[js.Dynamic].default.defs(
      projection.name,
      projection.proj
    )
  }

  proj4Mod.register(typings.proj4.mod.^.asInstanceOf[js.Dynamic].default)

  def toOlProj(projection: MapParamsProjection) = {
    new projectionMod.default(projectionMod.Options(projection.name)
      .setUnits(projection.unit)
      .setExtent(js.Tuple4(
        projection.extent.lift(0).getOrElse(0),
        projection.extent.lift(1).getOrElse(0),
        projection.extent.lift(2).getOrElse(0),
        projection.extent.lift(3).getOrElse(0),
      ))
    )
  }

  val wgs84Proj = toOlProj(wgs84)
  val projections = options.projections.map { projection => projection.name -> toOlProj(projection) }.toMap

  val defaultProjection = projections(options.defaultProjection)

}
