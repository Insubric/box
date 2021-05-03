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
case class FileSimpleWidget(widgetParams:WidgetParams) extends Widget with HasData with Logging {

  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._
  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  val field = widgetParams.field
  val data = widgetParams.prop

  val mime:Property[Option[String]] = Property(None)
  val source:Property[Option[String]] = Property(None)

  val uploadFilenameField:Option[String] = field.params.flatMap(_.getOpt("uploadFilenameField"))
  val downloadFilenameField:Option[String] = field.params.flatMap(_.getOpt("downloadFilenameField")).orElse(uploadFilenameField)
  val showDownload:Boolean = field.params.exists(_.js("showDownload") == Json.True)

  val filenameProp = uploadFilenameField.map{f => widgetParams.otherField(f)}

  data.listen({js =>
    val file = data.get.string
    if(file.length > 0 && file != FileUtils.keep) {
      val mime = file.take(1) match {
        case "/" => "image/jpeg"
        case "i" => "image/png"
        case "R" => "image/gif"
        case "J" => "application/pdf"
        case _ => "application/octet-stream"
      }
      this.mime.set(Some(mime))
      this.source.set(Some(s"data:$mime;base64,$file"))
    } else if(file == FileUtils.keep) {
      source.set(Some(FileUtils.keep))
      this.mime.set(None)
    } else {
      source.set(None)
      this.mime.set(None)
    }
  }, true)


  def url(idString:String):Option[(String,String)] = {
    JSONID.fromString(idString).map{ id =>
      val randomString = UUID.randomUUID().toString
      val u = s"/file/${widgetParams.metadata.entity}.${field.name}/$idString"
      (
        s"$u/thumb?rand=$randomString",
        s"$u?name=$name"
      )
    }
  }

  val urls = widgetParams.id.transform(x => x.flatMap(url))

  private def showFile = div(BootstrapCol.md(12),ClientConf.style.noPadding)(
    produceWithNested(mime.combine(source)((m,s) => (m,s))) {
      case ((Some(mime),source),nested) => if(mime.startsWith("image")) {
        div(
          source match {
            case None => frag()
            case Some(image) => img(src := image, ClientConf.style.maxFullWidth).render
          }
        ).render
      } else span("File loaded").render
      case ((None,Some(source)),nested) => div(
        nested(produce(urls) {
          case Some((thumb,download)) => div(
            img(src := Routes.apiV1(thumb),ClientConf.style.imageThumb),
            div(
              a("Download", ClientConf.style.boxButton, href := Routes.apiV1(download)),
            )
          ).render
          case _ => div().render
        })
      ).render
      case _ => div().render
    },
    div(BootstrapStyles.Visibility.clearfix)
  )




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

      filenameProp.foreach { field =>
        field.set(file.name.asJson)
      }

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

  override def create(params: WidgetParams): Widget = FileSimpleWidget(params)

}
