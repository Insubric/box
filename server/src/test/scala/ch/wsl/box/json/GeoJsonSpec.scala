package ch.wsl.box.json

import io.circe._
import io.circe.syntax._
import ch.wsl.box.BaseSpec
import org.locationtech.jts.geom.{Coordinate, GeometryFactory}


class GeoJsonSpec extends BaseSpec {
  import _root_.ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._

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
