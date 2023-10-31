package ch.wsl.box.client.geo

import ch.wsl.box.client.viewmodel.I18n
import ch.wsl.box.model.shared.{GeoJson, JSONQuery}
import ch.wsl.box.model.shared.GeoJson.{CRS, Geometry}
import ch.wsl.box.model.shared.geo.{Box2d, MapProjection}

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


case class MapParamsLayers(
                            name: String,
                            capabilitiesUrl: String,
                            layerId:String,
                            time:Option[String]
                          )

case class MapLookup(
                             color: String,
                             query: Option[JSONQuery],
                             entity:String,
                             kind:String,
                             column:String
                          ) {
  def id = s"$kind-$entity-$query-$column"
}

case class MapFormatters(
                          point:Option[I18n.Label],
                          multiPoint:Option[I18n.Label],
                          line:Option[I18n.Label],
                          multiLine:Option[I18n.Label],
                          polygon:Option[I18n.Label],
                          multiPolygon:Option[I18n.Label]
                        ) {

  import I18n._

  def geomToString(precision:Double,lang:String)(g:Geometry):String = {

    def asString(pattern:Option[I18n.Label]) = pattern.flatMap(_.lang(lang)) match {
      case Some(value) => g.format(value,precision)
      case None => g.toString(precision)
    }

    g match {
      case GeoJson.Point(coordinates,crs) => asString(point)
      case GeoJson.LineString(coordinates,crs) => asString(line)
      case GeoJson.Polygon(coordinates,crs) => asString(polygon)
      case GeoJson.MultiPoint(coordinates,crs) => asString(multiPoint)
      case GeoJson.MultiLineString(coordinates,crs) => asString(multiLine)
      case GeoJson.MultiPolygon(coordinates,crs) => asString(multiPolygon)
      case GeoJson.GeometryCollection(geometries,crs) => g.toString(precision)
    }
  }
}

case class MapParams(
                      features: MapParamsFeatures,
                      defaultProjection: String,
                      projections: Seq[MapProjection],
                      baseLayers: Option[Seq[MapParamsLayers]],
                      precision: Option[Double],
                      formatters: Option[MapFormatters],
                      enableSwisstopo: Option[Boolean],
                      lookups:Option[Seq[MapLookup]]
                    ) {
  def crs = CRS(defaultProjection)
  def bbox = Box2d.fromSeq(projections.find(_.name == defaultProjection).get.extent.get)
}

