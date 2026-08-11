package ch.wsl.box.rest.io.geotools

import ch.wsl.box.model.shared.GeoJson
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.locationtech.jts.geom.{Geometry, LineString, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon}

object GeotoolUtils {

  def geomSchema(builder:SimpleFeatureTypeBuilder,name:String,geo:Seq[Option[GeoJson.Geometry]],srid:Int) = {
    geo.toList.flatten.filterNot(_ == GeoJson.Empty).headOption match {
      case Some(value) => {
        value match {
          case geometry: GeoJson.SingleGeometry => geometry match {
            case GeoJson.Point(_, crs) => builder.add(name, classOf[Point], crs.srid)
            case GeoJson.LineString(_, crs) => builder.add(name, classOf[LineString], crs.srid)
            case GeoJson.Polygon(_, crs) => builder.add(name, classOf[Polygon], crs.srid)
          }
          case GeoJson.MultiPoint(_, crs) => builder.add(name, classOf[MultiPoint], crs.srid)
          case GeoJson.MultiLineString(_, crs) => builder.add(name, classOf[MultiLineString], crs.srid)
          case GeoJson.MultiPolygon(_, crs) => builder.add(name, classOf[MultiPolygon], crs.srid)
          case GeoJson.GeometryCollection(_, crs) => builder.add(name, classOf[Geometry], crs.srid)
        }
      }
      case None => builder.add(name, classOf[Geometry],srid)
    }
  }

}
