package ch.wsl.box.rest

import _root_.io.circe._
import _root_.io.circe.syntax._

import org.locationtech.jts.geom.{Coordinate, GeometryFactory}
import org.scalatest._

class GeoJsonSpec extends BaseSpec {
  import _root_.ch.wsl.box.rest.utils.JSONSupport._
  import _root_.io.circe.generic.auto._

  case class Test_row(id: Option[Int] = None, name: Option[String] = None, geom: Option[org.locationtech.jts.geom.Geometry] = None)


  "GeoJson package" should "go from Geometry to String" in {
    val g = Test_row(
      id = Some(1),
      name = Some("test"),
      geom = Some(new GeometryFactory().createPoint(new Coordinate(1,1)))
    )

    g.asJson.isNull shouldBe false

  }

}
