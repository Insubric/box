package ch.wsl.box.model.shared.geo


import ch.wsl.box.model.shared.GeoJson.{CRS, Coordinates, Polygon}
import ch.wsl.box.model.shared.JSONQuery

import java.util.UUID

case class Box2d(
                  xMin: Double,
                  yMin: Double,
                  xMax: Double,
                  yMax: Double
                ) {
  def toExtent():Seq[Double] = Seq(xMin,yMin,xMax,yMax)
  def toPolygon(crs:CRS):Polygon = Polygon(Seq(Seq(
    Coordinates(xMin, yMin),
    Coordinates(xMin, yMax),
    Coordinates(xMax, yMax),
    Coordinates(xMax, yMin),
    Coordinates(xMin, yMin)
  )), crs)
}

object Box2d{
  def fromSeq(s:Seq[Double]):Box2d = Box2d(
    s.lift(0).getOrElse(0),
    s.lift(1).getOrElse(0),
    s.lift(2).getOrElse(0),
    s.lift(3).getOrElse(0),
  )
}



case class MapMetadata(
                        id: UUID,
                        name: String,
                        parameters: Seq[String],
                        srid: MapProjection,
                        boundingBox: Box2d,
                        maxZoom: Double,
                        wmts: Seq[WMTS],
                        db: Seq[DbVector]
                      )
