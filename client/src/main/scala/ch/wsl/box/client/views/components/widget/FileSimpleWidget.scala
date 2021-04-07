package ch.wsl.box.client.views.components.widget


import java.util.UUID

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels, REST}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.views.components.Debug
import ch.wsl.box.model.shared._
import io.circe.Json
import io.udash._
import io.udash.bindings.Bindings
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLAnchorElement
import org.scalajs.dom.{Event, File, FileReader, window}
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Random


/**
  *
  * @param id
  * @param prop          holds the filename
  * @param field
  * @param entity
  */
case class FileSimpleWidget(id:Property[Option[String]], data:Property[Json], field:JSONField, entity:String) extends Widget with HasData with Logging {

  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._
  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  val mime:Property[String] = Property("application/octet-stream")
  val source:Property[Option[String]] = Property(None)

  data.listen({js =>
    val file = data.get.string
    if(file.length > 0) {
      val mime = file.take(1) match {
        case "/" => "image/jpeg"
        case "i" => "image/png"
        case "R" => "image/gif"
        case "J" => "application/pdf"
        case _ => "application/octet-stream"
      }
      this.mime.set(mime)
      this.source.set(Some(s"data:$mime;base64,$file"))
    } else {
      source.set(None)
      this.mime.set("application/octet-stream")
    }
  }, true)

  private def downloadFile(): Unit = {

    val link = dom.document.createElement("a").asInstanceOf[HTMLAnchorElement]
    val extension = mime.get.split("/")(1) match {
      case "octet-strem" => "bin"
      case s:String => s
    }
    val filename = s"download.$extension"
    link.href = source.get.get
    link.asInstanceOf[js.Dynamic].download = filename
    link.click()
  }

  private def showFile = showIf(source.transform(_.isDefined)){
    div(BootstrapCol.md(12),ClientConf.style.noPadding)(
      WidgetUtils.toLabel(field),
      produce(mime) { mime =>
        if(mime.startsWith("image")) {
          div(
            produce(source){
              case None => Seq()
              case Some(image) => img(src := image, ClientConf.style.maxFullWidth).render

            }
          ).render
        } else {
          button("Download", ClientConf.style.boxButton, onclick :+= ((e: Event) => downloadFile())).render
        }
      },
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }


  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

  val acceptMultipleFiles = Property(false)
  val selectedFiles = SeqProperty.blank[File]
  val fileInput = FileInput(selectedFiles, acceptMultipleFiles)("files",display.none).render

  selectedFiles.listen{ _.headOption.map{ file =>
    val reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      val result = reader.result.asInstanceOf[String]
      val token = "base64,"
      val index = result.indexOf(token)
      val base64 = result.substring(index+token.length)
      data.set(base64.asJson)
    }
  }}

  private def upload = {

    div(BootstrapCol.md(12),ClientConf.style.noPadding)(
      button("Upload",ClientConf.style.boxButton, onclick :+= ((e:Event) => fileInput.click()) ),
      showIf(source.transform(_.isDefined)){
        button("Delete",ClientConf.style.boxButtonDanger, onclick :+= ((e:Event) => if(window.confirm(Labels.form.removeMap)) data.set(Json.Null)) ).render
      },
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  override protected def show(): JsDom.all.Modifier = div(BootstrapCol.md(12),ClientConf.style.noPadding,
    showFile,
    div(BootstrapStyles.Visibility.clearfix),
  ).render

  override def edit() = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,
      if(noLabel) frag() else WidgetUtils.toLabel(field),
      showFile,
      upload,
      //autoRelease(produce(id) { _ => div(FileInput(selectedFile, Property(false))("file")).render }),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }


}

object FileSimpleWidgetFactory extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.simpleFile

  override def create(params: WidgetParams): Widget = FileSimpleWidget(params.id,params.prop,params.field,params.metadata.entity)

}
