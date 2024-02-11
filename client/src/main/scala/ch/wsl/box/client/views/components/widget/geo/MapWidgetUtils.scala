package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.geo.{BoxMapConstants, BoxOlMap, MapParams}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.viewmodel.{I18n, LangLabel}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.{GeoJson, JSONField}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import scalatags.JsDom.all._
import scribe.Logging
import ch.wsl.typings.ol.{layerBaseTileMod, layerMod, projProj4Mod, projProjectionMod, sourceMod}

import scala.scalajs.js


object MapWidgetUtils  extends Logging {


  def options(field:JSONField):MapParams = {
    val jsonOptions = ClientConf.mapOptions.deepMerge(field.params.getOrElse(JsonObject().asJson))
    jsonOptions.as[MapParams] match {
      case Right(value) => value
      case Left(error) => {
        logger.warn(s"Using default params - ${error} on json: $jsonOptions")
        BoxMapConstants.defaultParams
      }
    }
  }



}
