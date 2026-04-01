package ch.wsl.box.client.utils


import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport



object StripHtml {

  @js.native
  @JSImport("string-strip-html","stripHtml")
  private object StringStripHtml extends js.Object {
    def apply(input: String): js.Dynamic = js.native
  }

  def apply(s:String):String = {
    StringStripHtml(s).result.asInstanceOf[String]
  }

}
