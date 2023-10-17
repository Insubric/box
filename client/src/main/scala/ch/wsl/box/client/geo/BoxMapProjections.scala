package ch.wsl.box.client.geo

import ch.wsl.box.client.geo.BoxMapConstants.wgs84
import ch.wsl.box.client.geo.BoxMapProjections.toExtent
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.geo.{Box2d, MapProjection}
import scribe.Logging
import typings.ol.extentMod.Extent
import typings.ol.{layerBaseTileMod, layerMod, projMod, projProj4Mod, projProjectionMod, sourceMod}

import scala.scalajs.js

class BoxMapProjections(_projections:Seq[MapProjection],_defaultProjection:String,bbox:Box2d) extends Logging {

  typings.proj4.mod.^.asInstanceOf[js.Dynamic].default.defs(
    wgs84.name,
    wgs84.proj
  )


  _projections.map { projection =>
    //typings error need to map it manually
    typings.proj4.mod.^.asInstanceOf[js.Dynamic].default.defs(
      projection.name,
      projection.proj
    )
  }

  projProj4Mod.register(typings.proj4.mod.^.asInstanceOf[js.Dynamic].default)


  def toOlProj(projection: MapProjection) = {

    val projDef = projProjectionMod.Options(projection.name)
    //      .setUnits(projection.unit)


    projDef.setExtent(projMod.transformExtent(toExtent(bbox),_defaultProjection,projection.name))

    new projProjectionMod.default(projDef)
  }

  val wgs84Proj = toOlProj(wgs84)
  val projections = _projections.map { projection => projection.name -> toOlProj(projection) }.toMap

  val defaultProjection = projections(_defaultProjection)
}

object BoxMapProjections {
  def toExtent(s:Seq[Double]):Extent = js.Tuple4(
                s.lift(0).getOrElse(0),
                s.lift(1).getOrElse(0),
                s.lift(2).getOrElse(0),
                s.lift(3).getOrElse(0),
  )

  def toExtent(box2d: Box2d):Extent = toExtent(box2d.toExtent())
}

object BoxMapConstants {
  val defaultParams = MapParams(
    features = MapParamsFeatures(true, true, true, true, true, true, true),
    defaultProjection = "EPSG:3857",
    projections = Seq(MapProjection(
      name = "EPSG:3857",
      proj = "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs",
      extent = Some(Seq(-20026376.39, -20048966.10, 20026376.39, 20048966.10)),
//      unit = "m"
    )),
    None,
    None,
    None,
    None,
    None
  )

  val wgs84 = MapProjection(
    name = "EPSG:4326",
    proj = "+proj=longlat +datum=WGS84 +no_defs",
    extent = Some(Seq(-180, -90, 180, 90)),
//    unit = "m"
  )

  val openStreetMapLayer = new layerMod.Tile(layerBaseTileMod.Options().setSource(new sourceMod.OSM()))
}
