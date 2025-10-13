package ch.wsl.box.client.forms.widgets

import ch.wsl.box.client.geo.Control
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONMetadata, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import ch.wsl.typings.ol.coordinateMod.Coordinate
import ch.wsl.typings.ol.mod.MapBrowserEvent
import ch.wsl.typings.ol.pixelMod.Pixel
import ch.wsl.typings.ol.rendererMapMod
import io.circe.Json
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import org.scalajs.dom.{Event, HTMLButtonElement, HTMLOptionElement, HTMLSelectElement, MouseEvent, MouseEventInit, PointerEvent, PointerEventInit, document, window}
import org.scalatest.Assertion

import java.util.UUID
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import io.circe.parser._

import scala.scalajs.js.JSConverters._
import scala.util.Try

class MapWidgetTest extends TestBase {

  override val debug: Boolean = false

  val formName = "formName"
  val idField = "idField"
  val geomField = "geomField"


  val x = 2600000.0
  val y = 1100000.0

  val promise = Promise[Assertion]()


  class MockValues extends Values(loggerLevel){
    override def metadata: JSONMetadata = JSONMetadata.simple(UUID.randomUUID(),"form",formName,"it",Seq(
      JSONField.integer(idField,false),
      JSONField(JSONFieldTypes.GEOMETRY,geomField,nullable = false,params = Some(
        parse("""
          |
          |{
          |    "features": {
          |        "point": true,
          |        "multiPoint": false,
          |        "line": false,
          |        "multiLine": false,
          |        "polygon": true,
          |        "multiPolygon": true,
          |        "geometryCollection": true
          |    },
          |    "projections": [
          |        {
          |            "name": "EPSG:2056",
          |            "proj": "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs ",
          |            "unit": "m",
          |            "extent": [
          |               	2485869.5728, 1076443.1884, 2837076.5648, 1299941.7864
          |            ]
          |        }
          |    ],
          |    "defaultProjection": "EPSG:2056",
          |    "baseLayers": [
          |        {
          |            "name": "Swisstopo",
          |            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/2056/1.0.0/WMTSCapabilities.xml",
          |            "layerId": "ch.swisstopo.pixelkarte-farbe"
          |        },
          |        {
          |            "name": "SwissImage",
          |            "capabilitiesUrl": "https://wmts.geo.admin.ch/EPSG/2056/1.0.0/WMTSCapabilities.xml",
          |            "layerId": "ch.swisstopo.swissimage"
          |         }
          |     ],
          |     "precision": 0.01,
          |     "enableSwisstopo": true
          |}
          |""".stripMargin).toOption.get
        )
      )
    ),Seq("id"))


    override def insert(data: Json): Json = {
      val mapData = data.js(geomField).as[Geometry].toOption
      promise.tryComplete { Try{
        mapData.isDefined shouldBe true
        mapData.get.geomName shouldBe "POINT"
        mapData.get.crs.srid shouldBe 2056

        // I don't care about precise coordinates
        mapData.get.allCoordinates.head.x > 0 shouldBe true
        mapData.get.allCoordinates.head.y > 0 shouldBe true

      }}
      data
    }

  }

  override def values: Values = new MockValues

  def getPixel(c:Coordinate):Future[Pixel] = {
    val promise = Promise[Pixel]()

    def checkPixel():Unit = {
      val pixel = TestHooks.map.getPixelFromCoordinate(c)
      if(pixel != null) {
        promise.success(pixel)
      } else {
        window.setTimeout(() => checkPixel(),100)
      }
    }

    checkPixel()

    promise.future
  }

  "Map widget" should "be insertable when geom in required" in test{
    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", formName, "true", None, false))
      }
      _ <- waitElement(() => document.querySelector(s".${TestHooks.mapControlButton(Control.POINT)}"),"Waiting insert point button")
      pixel <-getPixel(js.Array(x,y))
      _ <- Future{
        val insertButton = document.querySelector(s".${TestHooks.mapControlButton(Control.POINT)}")
        insertButton.asInstanceOf[HTMLButtonElement].click()

        Seq("pointerdown","pointerup").foreach{ t =>

          val pe = new PointerEventInit{}
          pe.bubbles = Some(true).orUndefined
          pe.cancelable = Some(true).orUndefined
          pe.clientX = pixel(0)
          pe.clientY = pixel(1)
          pe.pointerType = "mouse"
          pe.button = 0

          TestHooks.map.getViewport().dispatchEvent(new PointerEvent(t,pe))
        }

        val mouseEvent = new MouseEventInit{}
        mouseEvent.bubbles = Some(true).orUndefined
        mouseEvent.cancelable = Some(true).orUndefined
        mouseEvent.clientX = pixel(0)
        mouseEvent.clientY = pixel(1)
        mouseEvent.button = 0

        TestHooks.map.getViewport().dispatchEvent(new MouseEvent("click",mouseEvent))


      }
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
      _ <- assertOrWait(document.getElementById(TestHooks.dataChanged) != null)
      _ <- Future{
        val idEl = document.querySelector(s".${TestHooks.formField(idField)}").asInstanceOf[HTMLInputElement]
        idEl.value = "1"
        idEl.onchange(new Event("change"))
      }
      _ = {
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      result <- promise.future
    } yield result
  }

}
