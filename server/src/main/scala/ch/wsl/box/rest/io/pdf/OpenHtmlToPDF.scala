package ch.wsl.box.rest.io.pdf

import java.io.ByteArrayOutputStream

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom

class OpenHtmlToPDF extends Pdf {
  override def render(html: String):  Array[Byte] = {
    val os = new ByteArrayOutputStream();
    val pdfRenderer = new PdfRendererBuilder
    pdfRenderer.withW3cDocument(html5ParseDocument(html), "/")
    pdfRenderer.toStream(os)
    pdfRenderer.run()
    os.close()
    println("rendered finished")
    os.toByteArray
  }

  def html5ParseDocument(html:String):org.w3c.dom.Document = {
    new W3CDom().fromJsoup(Jsoup.parse(html))
  }

}
