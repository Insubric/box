package ch.wsl.box.client.forms.childs

import ch.wsl.box.client.geo.{Control, MapUtils}
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.GeoJson.{CRS, Empty, Feature, FeatureCollection, Geometry}
import ch.wsl.box.model.shared.GeoTypes.GeoData
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, DbVectorProperties, MapMetadata, MapProjection, POLYGON, WMTS}
import ch.wsl.box.model.shared.{EntityKind, Filter, GeoJson, JSONField, JSONFieldTypes, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter, Layout, SharedLabels, WidgetsNames}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.scalajs.dom.{Event, HTMLButtonElement, HTMLOptionElement, HTMLSelectElement, document, window}
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}
import scribe.Level

import java.util.UUID
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import MapUtils._
import ch.wsl.box.client.Context.services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import org.scalatest.Assertion

import scala.util.Try

class MapChildWidgetTest extends TestBase {

  override val debug: Boolean = false
  override val waitOnAssertFail: Boolean = false

  override def loggerLevel: Level = Level.Error


  val promise = Promise[Assertion]()


  val idParent = "id"
  val parentName = "mapParent"
  val layerName = "Test DB"
  val map_name = "test_map"

  val proj = MapProjection("EPSG:21781","+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs ")

  val vectorLayerId = UUID.fromString("81e1fb9d-866c-419e-90b3-beb9391078e2")
  val vectorLayer = DbVector(id = vectorLayerId, name = layerName, entity = "fire", field = "polygon", entityPrimaryKey = Seq("fire_id"), srid = proj, geometryType = POLYGON, query = Some(JSONQuery.filterWith(JSONQueryFilter("fire_id", Some(Filter.EQUALS),fieldValue = Some("fire_id"), value = None))), extra = Json.Null, editable = true, zIndex = 2, order = 3, autofocus = true, color = "rgb(204,0,0)")


  class MockValues extends Values(loggerLevel){

    var _data:Json = Json.Null


    override def get(id: JSONID): Json = {
      _data
    }



    override def metadata: JSONMetadata = JSONMetadata.simple(values.id1,EntityKind.FORM.kind,parentName,"it",Seq(
      JSONField.number(idParent,nullable = false),
      JSONField(JSONFieldTypes.MAP,map_name,true,widget = Some(WidgetsNames.mapChild),map = Some(MapMetadata(
        id = UUID.randomUUID(),
        name = "test",
        parameters = Seq("fire_id"),
        srid = proj,
        boundingBox = Box2d(xMin = 485071.58, yMin = 74261.72, xMax = 837119.8, yMax = 299941.79),
        maxZoom = 12,
        wmts = Seq(
          WMTS(id = UUID.randomUUID(),name = "SwissTopo Farbe", capabilitiesUrl = "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml", layerId = "ch.swisstopo.pixelkarte-farbe", srid = proj, order = 1, zIndex = 0,extra = Json.Null),
          WMTS(id = UUID.randomUUID(),name = "SwissTopo Grau", capabilitiesUrl = "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml", layerId = "ch.swisstopo.pixelkarte-grau", srid = proj, order = 2, zIndex = 0,extra = Json.Null),
        ),
        db = Seq(
          vectorLayer
        ))
      ))
    ),Seq(idParent))

    override def insert(data: Json): Json = {
      _data = data
      val mapData = data.js(map_name).as[FeatureCollection].toOption
      promise.tryComplete { Try{
        mapData.isDefined shouldBe true
        mapData.get.features.size shouldBe 1
        mapData.get.features.head.geometry shouldBe Empty
      }}
      data
    }





    override def children(entity: String): Seq[JSONMetadata] = Seq()

    val polygon = GeoJson.Polygon(List(List(
      GeoJson.Coordinates(701298.315901753, 112883.97642683581),
      GeoJson.Coordinates(701311.2263166115, 112887.84955129334),
      GeoJson.Coordinates(701324.1367314699, 112891.72267575086),
      GeoJson.Coordinates(701332.7436747089, 112895.5958002084),
      GeoJson.Coordinates(701336.1864520045, 112907.64552074292),
      GeoJson.Coordinates(701334.0347161947, 112914.10072817214),
      GeoJson.Coordinates(701324.9974257938, 112922.27732424914),
      GeoJson.Coordinates(701315.0994410691, 112923.99871289692),
      GeoJson.Coordinates(701305.0917996713, 112943.13267918663),
      GeoJson.Coordinates(701294.4427772956, 112922.27732424914),
      GeoJson.Coordinates(701266.4568579326, 112937.25565243619),
      GeoJson.Coordinates(701275.5075021698, 112917.97385262966),
      GeoJson.Coordinates(701269.0522947407, 112910.65795087656),
      GeoJson.Coordinates(701273.786113522, 112893.87441156061),
      GeoJson.Coordinates(701271.2040305504, 112890.86198142698),
      GeoJson.Coordinates(701298.315901753, 112883.97642683581)
    )),crs = CRS("EPSG:21781"))

    override def geoData(entity: String, field: String):GeoData = {
      Seq(
        GeoJson.Feature(polygon)
      )
    }
  }

  override def values: Values = new MockValues

  "Map child in standalone mode" should "enable delete of a polygon" in test{
    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", parentName, "true", None, false))
      }
      _ <- waitElement(() => document.querySelector("option"),"Wating for Form to be loaded")
      _ = {
        document.getElementsByTagName("option").toSeq.filter(_.innerText == layerName).foreach{ el =>
          val layerValue = el.asInstanceOf[HTMLOptionElement].value
          val select = el.parentNode.asInstanceOf[HTMLSelectElement]
          select.value = layerValue
          select.onchange(new Event("change"))
          window.asInstanceOf[js.Dynamic].testMap = TestHooks.map
        }
      }
      _ <- waitElement(() => document.querySelector(s".${TestHooks.mapControlButton(Control.DELETE)}"),"Waiting delete polygon button")
      _ <- Future{
        val deleteButton = document.querySelector(s".${TestHooks.mapControlButton(Control.DELETE)}")
        deleteButton.asInstanceOf[HTMLButtonElement].click()

        val source = TestHooks.map.sourceOf(vectorLayer)
        source.foreach(_.clear())

        val idEl = document.querySelector(s".${TestHooks.formField(idParent)}").asInstanceOf[HTMLInputElement]
        idEl.value = "1"
        idEl.onchange(new Event("change"))

      }
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
      _ <- assertOrWait(document.getElementById(TestHooks.dataChanged) != null)
      _ = {
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      result <- promise.future
    } yield result
  }


}
