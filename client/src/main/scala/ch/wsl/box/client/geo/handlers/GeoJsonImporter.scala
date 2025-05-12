package ch.wsl.box.client.geo.handlers

import ch.wsl.box.client.geo.BoxMapProjections
import ch.wsl.box.client.services.{BoxFileReader, BrowserConsole}
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry}
import org.scalajs.dom.File

import scala.concurrent.{ExecutionContext, Future}

class GeoJsonImporter(proj:BoxMapProjections) {
  def read(file:File)(implicit ex:ExecutionContext):Future[Option[Geometry]] = {
    BoxFileReader.readAsText(file).map{ text =>
      for{
        js <- io.circe.parser.parse(text).toOption
        _ = BrowserConsole.log(js)
        geoJson <- FeatureCollection.decode(js).toOption
      } yield {
        val geometries = geoJson.features.map(_.geometry.toSingle)
        val geom  = Geometry.fromSimple(geometries.flatten)
        geom.convert(proj.convert(geoJson.crs.getOrElse(geom.crs).name,proj.default.name),proj.default.crs)
      }

    }
  }

}
