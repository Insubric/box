package ch.wsl.box.client.views.components.widget

import java.util.UUID

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, REST}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.views.components.Debug
import ch.wsl.box.model.shared._
import io.circe.Json
import io.udash._
import io.udash.bindings.Bindings
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.File
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.Future
import scala.util.Random


/**
  *
  * @param id
  * @param prop          holds the filename
  * @param field
  * @param entity
  */
case class FileWidget(id:ReadableProperty[Option[String]], data:Property[Json], field:JSONField, entity:String) extends Widget with HasData with Logging {

  import ch.wsl.box.client.Context._
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._
  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  val instanceId = UUID.randomUUID().toString

  def url(idString:String):Option[String] = {
    JSONID.fromString(idString).map{ id =>
      s"/file/${entity}.${field.file.get.file_field}/${idString}"
    }
  }

  val urlProp:Property[Option[String]] = Property(id.get.flatMap(url))

  val fileName:Property[String] = Property("")

  val selectedFile: SeqProperty[File] = SeqProperty(Seq.empty[File])


  autoRelease(id.listen({ idString =>
    selectedFile.set(Seq())
    fileName.set(data.get.string)
    val newUrl = idString.flatMap(url)
    if(urlProp.get != newUrl) {
      urlProp.set(newUrl)
    }
  },true))

  autoRelease(selectedFile.listen{ files =>
    logger.info(s"selected file changed ${files.map(_.name)}")
    data.set(files.headOption.map(_.name).asJson)
  })





  override def afterSave(result:Json, metadata: JSONMetadata) = {
    logger.debug(s"FileWidget afterSave json: $result")
    val jsonid = result.ID(metadata.keys)
    for{
      idfile <- Future.sequence{
        val r: Seq[Future[Int]] = selectedFile.get.map(services.rest.sendFile(_,jsonid.get,s"${metadata.entity}.${field.file.get.file_field}")).toSeq
        r
      }
    } yield {
      logger.info("image saved")
      //id.touch()
      result
    }
  }







  private def showImage = {
    logger.debug("showImage")
    autoRelease(produceWithNested(urlProp) { (url,nested) =>
      val randomString = UUID.randomUUID().toString
      url match {
        case Some(u) => div(
          //need to understand why is been uploaded two times
          img(src := Routes.apiV1(s"${u}/thumb?$randomString"),ClientConf.style.imageThumb) ,br,
          nested(produce(fileName) { name => a(href := Routes.apiV1(s"$u?name=$name"), name).render })
        ).render
        case None => div().render
      }
    })
  }

  override protected def show(): JsDom.all.Modifier = div(BootstrapCol.md(12),ClientConf.style.noPadding,
    label(field.title),
    showImage,
    div(BootstrapStyles.Visibility.clearfix),
  ).render

  override def edit() = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,
      WidgetUtils.toLabel(field),
      showImage,
      autoRelease(produce(id) { _ => div(FileInput(selectedFile, Property(false))("file")).render }),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }


}

object FileWidgetFactory extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.fileWithPreview

  override def create(params: WidgetParams): Widget = FileWidget(params.id,params.prop,params.field,params.metadata.entity)
}
