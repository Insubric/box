package ch.wsl.box.client.services

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.model.shared.{ExportMode, JSONQuery}
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

import scala.scalajs.js.URIUtils
import JSONQuery._
import ch.wsl.box.client.utils.ImageUtils
import ch.wsl.typings.jspdfAutotable.anon.PartialStyles
import ch.wsl.typings.jspdfAutotable.mod.{HorizontalPageBreakBehaviourType, RowInput, UserOptions}
import kantan.csv._
import kantan.csv.ops._

import scala.concurrent.ExecutionContext
import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import scalatags.JsDom.all._
import ch.wsl.typings.jspdf.mod.jsPDF
import com.avsystem.commons.Future

object PDF {

  import ch.wsl.box.client.Context._


  def renderTable(title:String,header:Seq[String],body:Seq[Seq[String]])(implicit ec:ExecutionContext) = {

    val logo = UI.logo match {
      case Some(l) => for{
        img <- services.httpClient.getBlob(ClientConf.frontendUrl + l)
        resized <- ImageUtils.setHeight(img,60)
        base64 <- BoxFileReader.readAsDataURL(resized)
        dim <- ImageUtils.dimensions(base64)
      } yield {
        println(dim)
        Some((base64,dim))
      }
      case None => Future.successful(None)
    }

    logo.foreach { logo =>

      val doc = new jsPDF(ch.wsl.typings.jspdf.jspdfStrings.landscape,ch.wsl.typings.jspdf.jspdfStrings.mm)

      val data = body.map(_.toJSArray).toJSArray.asInstanceOf[js.Array[RowInput]]

      ch.wsl.typings.jspdfAutotable.mod.default(doc, UserOptions()
        .setHead(js.Array(header.toJSArray).asInstanceOf[js.Array[RowInput]])
        .setBody(data)
        .setWillDrawPage(data => {
          doc.setFontSize(12)
          doc.setTextColor(40)
          logo.foreach { case (i,d) =>
            println(d)
            doc.addImage(i, "JPEG", data.settings.margin.left, 6, d.scaled_w(10),10)
          }
          doc.text(s"${UI.title.getOrElse("Box Framework")} -  $title", data.settings.margin.left + 10 + logo.map(_._2.scaled_w(10)).getOrElse(0), 12)
          ()
        })
        .setMargin(js.Array[Double](20, 10, 10, 10))
        .setStyles(PartialStyles().setCellPadding(0.5).setFontSize(9))
        .setHeadStyles(PartialStyles().setFillColor(ClientConf.colorMain).setTextColor(ClientConf.colorMainText))
        .setHorizontalPageBreak(true)
        .setHorizontalPageBreakRepeat(0)
        .setHorizontalPageBreakBehaviour(HorizontalPageBreakBehaviourType.immediately)
      )

      val pages: Int = doc.internal.asInstanceOf[js.Dynamic].getNumberOfPages().asInstanceOf[Int]
      for (i <- 1 to pages) {
        doc.setFontSize(8)
        val h = doc.internal.pageSize.getHeight()
        val w = doc.internal.pageSize.getWidth()
        doc.setPage(i)
        doc.text(s"$i/$pages", w - 30, h - 10)
      }

      doc.save(s"$title.pdf")
    }
  }

  def table(kind:String,modelName:String,fields:Seq[String],query:JSONQuery)(implicit ec:ExecutionContext) = {
    val csv = Routes.apiV1(
      s"/$kind/${services.clientSession.lang()}/$modelName/csv?fk=${ExportMode.RESOLVE_FK}&fields=${fields.mkString(",")}&q=${URIUtils.encodeURI(query.asJson.noSpaces)}".replaceAll("\n","")
    )
    services.httpClient.get[String](csv).map{ result =>
      result.asUnsafeCsvReader[Seq[String]](rfc).toList match {
        case header :: body => renderTable(modelName,header,body)
      }


    }

  }
}
