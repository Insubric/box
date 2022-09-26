package ch.wsl.box.rest

import ch.wsl.box.model.shared.{JSONID, JSONKeyValue}



class EnhancedModelSpec extends BaseSpec {
  import _root_.ch.wsl.box.rest.utils.JSONSupport._
  import _root_.io.circe.generic.auto._

  case class Test_row(id: Option[Int] = None, name: Option[String] = None)



}
