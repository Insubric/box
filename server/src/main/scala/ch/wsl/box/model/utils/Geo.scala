package ch.wsl.box.model.utils

import ch.wsl.box.model.shared.GeoJson.{CRS, Coordinates}
import org.locationtech.jts.geom.Geometry
import scribe.Logging

import scala.util.{Failure, Success, Try}

object Geo extends Logging {

  def fromWKT(wkt:String): Option[Geometry] = {
    Try(new org.locationtech.jts.io.WKTReader().read(wkt)) match {
      case Failure(exception) => {
        logger.warn(exception.getMessage)
        None
      }
      case Success(value) => Some(value)
    }
  }

  def fromEWKT(ewkt:String) = {
    ewkt.split(";").toList match {
      case srid :: geom :: Nil => Try{
        val g = fromWKT(geom).get
        g.setSRID(srid.stripPrefix("SRID=").toInt)
        g
      }.toOption
      case _ => fromWKT(ewkt)
    }
  }
}
