package ch.wsl.box.rest

import ch.wsl.box.model.shared.{JSONID, JSONKeyValue}
import ch.wsl.box.rest.logic.EnhancedModel
import org.scalatest.{FlatSpec, Matchers}



class EnhancedModelSpec extends FlatSpec with Matchers {
  import _root_.ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._

  case class Test_row(id: Option[Int] = None, name: Option[String] = None)


  "EnhancedModel" should "return the JSONID of a simple model" in {
    val m = Test_row(
      id = Some(1),
      name = Some("test")
    )

    new EnhancedModel(m).ID(Seq("id")) shouldBe JSONID(Vector(JSONKeyValue("id","1")))

  }

}
