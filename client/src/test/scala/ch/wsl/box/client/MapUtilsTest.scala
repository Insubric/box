package ch.wsl.box.client

import ch.wsl.box.client.geo.{MapParamsFeatures, MapUtils}
import ch.wsl.box.model.shared.GeoJson

class MapUtilsTest extends TestBase {

  import GeoJson._



  "MapUtils" should "factor geometries" in {
    val original =
      """
        |{"type":"FeatureCollection","features":[{"geometry":{"crs":{"type":"name","properties":{"name":"EPSG:21781"}},"type":"MultiPolygon","coordinates":[[[[688304.8679199219,154272.203125],[688289.9666748047,154280.1616821289],[688300.2958984375,154297.4337158203],[688303.0053100586,154305.73107910156],[688307.5772705078,154311.65789794922],[688311.4719238281,154316.9071044922],[688319.5999145508,154316.6109008789],[688337.5493164062,154315.55249023438],[688330.7125244141,154303.64630126953],[688304.8679199219,154272.203125]]]]},"type":"Feature","properties":null}]}
        |""".stripMargin

    val fc = io.circe.parser.parse(original).toOption.get.as[FeatureCollection].toOption.get

    val geoms = MapUtils.factorGeometries(fc.features.map(_.geometry),MapParamsFeatures(),fc.features.head.geometry.crs)

    assert(geoms.isDefined)

  }

}