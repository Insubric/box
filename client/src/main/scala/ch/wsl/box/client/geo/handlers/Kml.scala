package ch.wsl.box.client.geo.handlers

import ch.wsl.box.client.geo.BoxMapProjections
import ch.wsl.box.client.services.{BoxFileReader, BrowserConsole}
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry}
import org.scalajs.dom.File
import ch.wsl.typings.ol
import ch.wsl.typings.ol.formatFeatureMod.ReadOptions
import ch.wsl.typings.ol.{formatGeoJSONMod, formatMod}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

class Kml(proj:BoxMapProjections) {
  def read(file:File)(implicit ex:ExecutionContext):Future[Option[Geometry]] = {
    BoxFileReader.readAsText(file).map{ text =>

      val options = ReadOptions().setFeatureProjection(proj.defaultProjection)
      val features = new formatMod.KML().readFeatures(text,options).asInstanceOf[js.Array[ol.renderFeatureMod.default]]
      for{
        feat <- io.circe.scalajs.convertJsToJson(new formatGeoJSONMod.default().writeFeaturesObject(features).asInstanceOf[js.Any]).toOption
        collection <- FeatureCollection.decode(feat) match {
          case Left(value) => {
            println(value.toString())
            None
          }
          case Right(value) => Some(value)
        }
      } yield {
        val single = collection.features.map(_.geometry.toSingle)
        Geometry.fromSimple(single.flatten)
      }
    }

  }

}
