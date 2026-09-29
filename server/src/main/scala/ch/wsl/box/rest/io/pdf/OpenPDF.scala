package ch.wsl.box.rest.io.pdf

import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.openpdf.pdf.ITextRenderer
import org.openpdf.resource.{HtmlParserConfig, HtmlResource}

import java.io.ByteArrayOutputStream

class OpenPDF extends Pdf {

  override def render(html: String):  Array[Byte] = {
    val os = new ByteArrayOutputStream()

    val doc = Jsoup.parse(html.linesIterator.mkString(""))
    val renderer = new ITextRenderer
    renderer.setDocument(new W3CDom().fromJsoup(doc))
    renderer.layout
    renderer.createPDF(os)


    os.close()
    println("rendered finished")
    os.toByteArray
  }


}
