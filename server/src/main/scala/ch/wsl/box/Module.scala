package ch.wsl.box

import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import wvlet.airframe._

object Module {
  val prod = newDesign
    .bind[Connection].to[ConnectionConfImpl]
}
