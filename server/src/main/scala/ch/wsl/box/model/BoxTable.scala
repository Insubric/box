package ch.wsl.box.model

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

trait BoxTable[T] { t:Table[T] =>
  def isBox:Boolean = t.schemaName.contains(Registry.box().schema)
  def registry = if(isBox) Registry.box() else Registry()
}
