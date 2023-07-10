package ch.wsl.box.client.views.components.widget


import java.util.UUID
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.LoginPopup.{body, header}
import ch.wsl.box.client.services.{ClientConf, Labels, REST}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.views.components.Debug
import ch.wsl.box.model.shared._
import io.circe.Json
import io.udash._
import io.udash.bindings.Bindings
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.BackdropType
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.properties.single.Property
import org.scalajs.dom
import org.scalajs.dom.raw.{DragEvent, HTMLAnchorElement}
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
  def data = widgetParams.prop

  val mime:Property[Option[String]] = Property(None)
  val source:Property[Option[String]] = Property(None)

  val uploadFilenameField:Option[String] = field.params.flatMap(_.getOpt("uploadFilenameField"))
  val downloadFilenameField:Option[String] = field.params.flatMap(_.getOpt("downloadFilenameField")).orElse(uploadFilenameField)
  val showDownload:Boolean = field.params.exists(_.js("showDownload") == Json.True)

  val filenameProp = uploadFilenameField.map{f => widgetParams.otherField(f)}

  data.listen({js =>
    def file = data.get.string
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


  override def killWidget(): Unit = {
    source.set(None)
    super.killWidget()
  }

  def url(data:Json):Option[(String,String)] = {
    JSONID.fromData(data,widgetParams.metadata).map{ id =>
      val randomString = UUID.randomUUID().toString
      val idString = id.asString
      val name = downloadFilenameField.flatMap(data.getOpt) match {
        case Some(name) => name
        case None => s"${widgetParams.metadata.name}_$idString"
      }
      val u = s"/file/${widgetParams.metadata.entity}.${field.name}/$idString"
      (
        s"$u/thumb?rand=$randomString",
        s"$u?name=$name"
      )
    }
  }

  val urls = widgetParams.allData.transform(url)

  private def showFile(nested:Binding.NestedInterceptor) = div(ClientConf.style.noPadding)(
    nested(produceWithNested(mime.combine(source)((m,s) => (m,s))) {
      case ((Some(mime),source),nested) => if(mime.startsWith("image")) {
        div(
          source match {
            case None => frag()
            case Some(image) => img(src := image, ClientConf.style.maxFullWidth).render
          }
        ).render
      } else div(textAlign.center,marginTop := 20.px,
        div(Icons.fileOk(50)),
        span("File loaded")
      ).render
      case ((None,Some(source)),nested) => div(
        nested(produce(urls) {
          case Some((thumb,_download)) => {

            val url:Property[String] = Property("")

            val modal: UdashModal = UdashModal(
              modalSize = Some(Size.Large).toProperty,
              backdrop = BackdropType.Active.toProperty
            )(
              headerFactory = None,
              bodyFactory = Some((interceptor) => img(nested(src.bind(url))).render),
              footerFactory = None
            )

            div(
              img(src := Routes.apiV1(thumb),ClientConf.style.imageThumb, onclick :+= ((e:Event) => {
                e.preventDefault()
                url.set(Routes.apiV1(_download))
                modal.show()
              })),
              modal
              //            div(
              //              a(Icons.download, ClientConf.style.boxIconButton, href := Routes.apiV1(_download),attr("download") := "download"),
              //            )
            ).render
          }
          case _ => div().render
        })
      ).render
      case _ => div().render
    }),
    div(BootstrapStyles.Visibility.clearfix)
  )




  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

  val acceptMultipleFiles = Property(false)
  val selectedFiles = SeqProperty.blank[File]
  val fileInput = FileInput(selectedFiles, acceptMultipleFiles)("files",display.none,onfocus :+= {(e:Event) =>
    e.preventDefault()
  }).render


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
      fileInput,
      produce(urls) {
        case Some((thumb,_download)) =>
          WidgetUtils.addTooltip(Some("Download")){
            a(Icons.download, ClientConf.style.boxIconButton, href := Routes.apiV1(_download),attr("download") := "download").render
          }._1
        case _ => span().render
      },
      WidgetUtils.addTooltip(Some("Upload"))(button(Icons.upload,ClientConf.style.boxIconButton, onclick :+= {(e:Event) =>
        fileInput.click()
        e.preventDefault()
      } ).render)._1,
      showIf(source.transform(_.isDefined)){
        WidgetUtils.addTooltip(Some("Delete")) {
          button(Icons.trash, ClientConf.style.boxIconButtonDanger, onclick :+= { (e: Event) =>
            if (window.confirm(Labels.form.removeMap)) data.set(Json.Null)
            e.preventDefault()
          }).render
        }._1
      },
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  private val dropHandler = (e:DragEvent) => {
    e.preventDefault()

    if(e.dataTransfer.files.length > 0) {
      val files = for(i <- 0 until e.dataTransfer.files.length) yield e.dataTransfer.files(i)
      selectedFiles.set(files)
    }

    dragging.set(false)

  }

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div(BootstrapCol.md(12),ClientConf.style.noPadding,
    showFile(nested),
    div(BootstrapStyles.Visibility.clearfix),
  ).render

  val dragging:Property[Boolean] = Property(false)

  val dropZoneId = "dz-" + UUID.randomUUID().toString
  def dropZone(nested:Binding.NestedInterceptor) = div(
    ClientConf.style.dropFileZone,
    id := dropZoneId,
    showFile(nested),
    ondrop :+= dropHandler,
    ondragover :+= {(e:Event) => dragging.set(true); e.preventDefault()},
    ondragenter :+= ((_:Event) => dragging.set(true)),
    ondragleave :+= ((_:Event) => dragging.set(false)),
    p(Labels.form.drop),
  ).render

  autoRelease(dragging.listen{
    case true => dom.document.getElementById(dropZoneId).classList.add(ClientConf.style.dropFileZoneDropping.htmlClass)
    case false => dom.document.getElementById(dropZoneId).classList.remove(ClientConf.style.dropFileZoneDropping.htmlClass)
  })

  override def edit(nested:Binding.NestedInterceptor) = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,
      if(noLabel) frag() else WidgetUtils.toLabel(field,WidgetUtils.LabelLeft),
      dropZone(nested:Binding.NestedInterceptor),
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
