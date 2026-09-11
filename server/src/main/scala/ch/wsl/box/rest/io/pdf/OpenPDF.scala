package ch.wsl.box.rest.io.pdf

import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.openpdf.pdf.ITextRenderer
import org.openpdf.resource.{HtmlParserConfig, HtmlResource}

import java.io.ByteArrayOutputStream

class OpenPDF extends Pdf {

  override def render(html: String):  Array[Byte] = {
    val os = new ByteArrayOutputStream()

//    // Parse with custom configuration
//    val config = HtmlParserConfig.builder
//      .reportErrors(true)
//      .allowSelfClosingTags(true)
//      .encoding("UTF-8").build
//    val resource = HtmlResource.load(html, config)


    val doc = Jsoup.parse(html.linesIterator.mkString(""))
    println(doc.outerHtml)
    val renderer = new ITextRenderer
    renderer.setDocument(new W3CDom().fromJsoup(doc))
    //renderer.setDocumentFromString(doc.outerHtml)
    renderer.layout
    renderer.createPDF(os)


    os.close()
    println("rendered finished")
    os.toByteArray
  }

  private def html5ParseDocument(html:String):org.w3c.dom.Document = {
    new W3CDom().fromJsoup(Jsoup.parse(html))
  }

}
