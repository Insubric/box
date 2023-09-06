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

}