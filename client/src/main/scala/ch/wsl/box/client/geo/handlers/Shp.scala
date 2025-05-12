package ch.wsl.box.client.geo.handlers

import ch.wsl.box.client.geo.{BoxMapProjections, MapUtils}
import ch.wsl.box.client.services.{BoxFileReader, BrowserConsole}
import ch.wsl.box.model.shared.GeoJson.{CRS, FeatureCollection, Geometry}
import ch.wsl.typings.ol.{extentMod, geomGeometryMod, layerBaseVectorMod, layerMod, projProjectionMod, sourceMod, sourceVectorMod}
import ch.wsl.typings.ol.extentMod.Extent
import ch.wsl.typings.ol.formatFeatureMod.ReadOptions
import ch.wsl.typings.ol.formatMod.GeoJSON
import org.scalajs.dom.{File, FileReader}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

class Shp(proj:BoxMapProjections) {
  def read(file:File)(implicit ex:ExecutionContext):Future[Option[Geometry]] = {
    for {
      shp <- BoxFileReader.readAsArrayBuffer(file)
      geometry <- ch.wsl.typings.shapefile.mod.read(shp).toFuture.map { gj =>

        val projName: String = gj.bbox.toOption.flatMap { bbox =>
          val b = bbox.asInstanceOf[js.Tuple4[Double, Double, Double, Double]]
          val e: Extent = js.Array(b._1, b._2, b._3, b._4)
          proj.projections.find{ case (n,p) => extentMod.intersects(p.getExtent(), e) }.map(_._1)
        }.getOrElse(proj.default.name)

        for{
          json <- io.circe.scalajs.convertJsToJson(gj).toOption
          jsonWithCRS <- MapUtils.attachCRS(json, CRS(projName))
          result <- FeatureCollection.decode(jsonWithCRS).toOption
        } yield {
          val simpleGeom = result.features.map(_.geometry.toSingle)
          Geometry.fromSimple(simpleGeom.flatten).convert(proj.convert(projName,proj.default.name),proj.default.crs)
        }

      }
    } yield geometry
  }
}
