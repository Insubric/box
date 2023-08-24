package ch.wsl.box.client.geo

import ch.wsl.box.client.geo.BoxMapConstants.wgs84
import scribe.Logging
import typings.ol.{layerBaseTileMod, layerMod, projProj4Mod, projProjectionMod, sourceMod}

import scala.scalajs.js

class BoxMapProjections(options:MapParams) extends Logging {

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

  projProj4Mod.register(typings.proj4.mod.^.asInstanceOf[js.Dynamic].default)


  def toOlProj(projection: MapParamsProjection) = {
    new projProjectionMod.default(projProjectionMod.Options(projection.name)
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

object BoxMapConstants {
  val defaultParams = MapParams(
    features = MapParamsFeatures(true, true, true, true, true, true, true),
    defaultProjection = "EPSG:3857",
    projections = Seq(MapParamsProjection(
      name = "EPSG:3857",
      proj = "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs",
      extent = Seq(-20026376.39, -20048966.10, 20026376.39, 20048966.10),
      unit = "m"
    )),
    None,
    None,
    None,
    None
  )

  val wgs84 = MapParamsProjection(
    name = "EPSG:4326",
    proj = "+proj=longlat +datum=WGS84 +no_defs",
    extent = Seq(-180, -90, 180, 90),
    unit = "m"
  )

  val openStreetMapLayer = new layerMod.Tile(layerBaseTileMod.Options().setSource(new sourceMod.OSM()))
}
