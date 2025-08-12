package ch.wsl.box.client

import ch.wsl.box.model.shared.GeoJson

class GeoJsonCodecTest extends TestBase {

  import GeoJson._

  "GeoJSON" should "encoded and decoded" in {
    val original = """
        |{
        |  "type": "Feature",
        |  "geometry" : {
        |    "type" : "Point",
        |    "coordinates" : [
        |      98728.5390625,
        |      720030.8125
        |    ]
        |  },
        |  "properties" : {
        |    "y" : 720030.8125,
        |    "featureId" : "5192",
        |    "zoomlevel" : 4294967295,
        |    "num" : 1,
        |    "id" : 1612,
        |    "origin" : "gg25",
        |    "lon" : 8.988879203796387,
        |    "detail" : "lugano ti",
        |    "geom_quadindex" : "032",
        |    "lat" : 46.0294303894043,
        |    "geom_st_box2d" : "BOX(712804.206999776 88658.77601225587,727694.7229997851 108825.19600984111)",
        |    "weight" : 6,
        |    "x" : 98728.5390625,
        |    "rank" : 2,
        |    "label" : "<b>Lugano (TI)</b>"
        |  }
        |}
        |""".stripMargin

      io.circe.parser.parse(original) match {
        case Left(value) => fail(value)
        case Right(value) => value.as[Option[GeoJson.Feature]] match {
          case Left(value) => {
            println(value)
            fail(value)
          }
          case Right(value) => {
            assert(value.get.geometry.allCoordinates.head.x == 98728.5390625)
          }
        }

    }

  }


  "Feature Collection" should "be decoded" in {
    val original =
      """
        |{"type":"FeatureCollection","features":[{"geometry":{"crs":{"type":"name","properties":{"name":"EPSG:21781"}},"type":"MultiPolygon","coordinates":[[[[688304.8679199219,154272.203125],[688289.9666748047,154280.1616821289],[688300.2958984375,154297.4337158203],[688303.0053100586,154305.73107910156],[688307.5772705078,154311.65789794922],[688311.4719238281,154316.9071044922],[688319.5999145508,154316.6109008789],[688337.5493164062,154315.55249023438],[688330.7125244141,154303.64630126953],[688304.8679199219,154272.203125]]]]},"type":"Feature","properties":null}]}
        |""".stripMargin

    io.circe.parser.parse(original) match {
      case Left(value) => fail(value)
      case Right(value) => value.as[Option[GeoJson.FeatureCollection]] match {
        case Left(value) => {
          println(value)
          fail(value)
        }
        case Right(value) => {
          assert(value.get.features.head.geometry.crs.srid == 21781)
        }
      }

    }

  }

}