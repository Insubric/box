package ch.wsl.box.client.vendors

import ch.wsl.typings.electricSqlPglite.distPgliteBdaQNAoWMod.BasePGlite
import org.scalajs.dom.{Blob, Worker}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("@electric-sql/pglite/worker", "PGliteWorker")
@js.native
class PGliteWorker(worker:Worker) extends js.Object with BasePGlite
