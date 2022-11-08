package ch.wsl.box.json

import io.circe._
import io.circe.syntax._
import ch.wsl.box.BaseSpec
import org.locationtech.jts.geom.{Coordinate, GeometryFactory}


class GeoJsonSpec extends BaseSpec {
  import io.circe.generic.auto._
  import geotrellis.vector.io.json.Implicits._

  case class Test_row(id: Option[Int] = None, name: Option[String] = None, geom: Option[org.locationtech.jts.geom.Geometry] = None)


  "GeoJson package" should "go from Geometry to String" in {
    val g = Test_row(
      id = Some(1),
      name = Some("test"),
      geom = Some(new GeometryFactory().createPoint(new Coordinate(1,1)))
    )

    g.asJson.isNull shouldBe false

  }

  it should "serialize polygon" in {
    val g = Test_row(
      id = Some(1),
      name = Some("test"),
      geom = Some(new GeometryFactory().createPolygon(Seq(new Coordinate(1,1),new Coordinate(0,1),new Coordinate(1,0),new Coordinate(1,1)).toArray))
    )

    println(g.asJson.toString())
    g.asJson.isNull shouldBe false
  }

}
