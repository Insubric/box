package ch.wsl.box.client.views.components.widget


import java.util.UUID
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.LoginPopup.{body, header}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels, REST}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.views.components.Debug
import ch.wsl.box.model.shared._
import ch.wsl.typings.compressorjs
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
import org.scalajs.dom.{Blob, Event, File, FileReader, window}
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

  case class CompressorOptions(quality:Double,maxWidth:Option[Double],maxHeight:Option[Double])
  import io.circe.generic.auto._
  val compressionOptions = field.params.flatMap(_.js("compressor").as[CompressorOptions].toOption)

  val uploadFilenameField:Option[String] = field.params.flatMap(_.getOpt("uploadFilenameField"))
  val downloadFilenameField:Option[String] = field.params.flatMap(_.getOpt("downloadFilenameField")).orElse(uploadFilenameField)
  val showDownload:Boolean = field.params.exists(_.js("showDownload") == Json.True)

  val filenameProp = uploadFilenameField.map{f => widgetParams.otherField(f)}

  def extractMime(file:String):String = file.take(1) match {
    case "/" => "image/jpeg"
    case "i" => "image/png"
    case "R" => "image/gif"
    case "J" => "application/pdf"
    case _ => "application/octet-stream"
  }

  data.listen({js =>
    def file = data.get.string
    if(file.length > 0 && !FileUtils.isKeep(file)) {
      this.mime.set(Some(extractMime(file)))
      this.source.set(Some(s"data:$mime;base64,$file"))
    } else if(FileUtils.isKeep(file)) {
      source.set(Some(file))
      this.mime.set(Some(FileUtils.extractMime(file)))
    } else {
      source.set(None)
      this.mime.set(None)
    }
  }, true)


  override def killWidget(): Unit = {
    source.set(None)
    super.killWidget()
  }

  def url(data:Json):Option[String] = {
    JSONID.fromData(data,widgetParams.metadata).map{ id =>
      val idString = id.asString
      val name = downloadFilenameField.flatMap(data.getOpt) match {
        case Some(name) => name
        case None => s"${widgetParams.metadata.name}_$idString"
      }
      val u = s"/file/${widgetParams.metadata.entity}.${field.name}/$idString"
      val randomString = UUID.randomUUID().toString
      s"$u?name=$name&r=$randomString"
    }
  }

  val idString:Property[Option[String]] = Property(None)
  autoRelease{
    widgetParams.allData.listen({data =>
      idString.set(JSONID.fromData(data,widgetParams.metadata).map(_.asString))
    },true)
  }

  val thumbUrl = idString.transform{ _.map{ idString =>
    val randomString = UUID.randomUUID().toString
    val u = s"/file/${widgetParams.metadata.entity}.${field.name}/$idString"
    s"$u/thumb?rand=$randomString"
  }}

  val downloadUrl = widgetParams.allData.transform(url)

  private def showFile(nested:Binding.NestedInterceptor) = div(ClientConf.style.noPadding)(
    nested(produceWithNested(mime.combine(source)((m,s) => (m,s))) {
      case ((Some(mime),Some(file)),nested) if !FileUtils.isKeep(file) => if(mime.startsWith("image")) {
        div(img(src := file, ClientConf.style.maxFullWidth)).render
      } else div(textAlign.center,marginTop := 20.px,
        div(Icons.fileOk(50),file.take(30)),
        span("File loaded")
      ).render
      case ((Some(mime),Some(file)),nested) if FileUtils.isKeep(file) => div(
        nested(produce(thumbUrl.combine(downloadUrl){ case (x,y) => (x,y)}) {
          case (Some(thumb),Some(_download)) => {

            val url:Property[String] = Property("")

            val modal: UdashModal = UdashModal(
              modalSize = Some(Size.Large).toProperty,
              backdrop = BackdropType.Active.toProperty
            )(
              headerFactory = None,
              bodyFactory = Some((interceptor) => {
                if(mime == "application/pdf") {
                  iframe(attr("src").bind(url.transform(f => s"${Routes.baseUri}pdf/web/viewer.html?file="+f)), ClientConf.style.fullWidth, ClientConf.style.fullHeight).render
                } else {
                  img(nested(src.bind(url))).render
                }
              }),
              footerFactory = None
            )

            div(
              img(src := Routes.apiV1(thumb),ClientConf.style.imageThumb, onclick :+= ((e:Event) => {
                if(mime.startsWith("image") || mime == "application/pdf") {
                  e.preventDefault()
                  url.set(Routes.apiV1(_download))
                  modal.show()
                } else {
                  window.open(Routes.apiV1(_download))
                }
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


  def readFile(file:Blob): Unit = {
    val reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      val result = reader.result.asInstanceOf[String]
      val token = "base64,"
      val index = result.indexOf(token)
      val base64 = result.substring(index+token.length)


      data.set(base64.asJson)
    }
  }

  selectedFiles.listen{ _.headOption.map{ file =>
    BrowserConsole.log(file)
    mime.set(Some(file.`type`))

    filenameProp.foreach { field =>
      field.set(file.name.asJson)
    }

    compressionOptions match {
      case Some(opt) if file.`type`.contains("image") => {
        val options = compressorjs.Compressor.Options()
          .setRetainExif(true)
          .setQuality(opt.quality)
          .setSuccess { file =>
            BrowserConsole.log(file.asInstanceOf[js.Any])
            readFile(file.asInstanceOf[Blob])
          }

        opt.maxWidth.foreach(options.setMaxWidth)
        opt.maxHeight.foreach(options.setMaxHeight)

        new compressorjs.mod.default(file,options)
      }
      case _ => readFile(file)
    }


  }}

  private def upload = {

    div(BootstrapCol.md(12),ClientConf.style.noPadding)(
      fileInput,
      produce(downloadUrl) {
        case Some(_download) =>
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
