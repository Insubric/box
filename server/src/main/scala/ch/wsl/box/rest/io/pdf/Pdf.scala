package ch.wsl.box.rest.io.pdf

import akka.stream.scaladsl.Source
import akka.util.ByteString

object Pdf {

  private val pdf:Pdf = new OpenHtmlToPDF();

  def render(html:String): Array[Byte] = pdf.render(html)
}

trait Pdf{
  def render(html:String): Array[Byte]
}
