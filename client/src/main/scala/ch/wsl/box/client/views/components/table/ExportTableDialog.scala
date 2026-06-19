package ch.wsl.box.client.views.components.table

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels, PDF}
import ch.wsl.box.model.shared.ExportTableFormat.GeoPackage
import ch.wsl.box.model.shared.{EntityKind, ExportMode, ExportTableFormat, GeometryTableFormat, JSONField, JSONFieldTypes, JSONMetadata, JSONQuery}
import io.circe.syntax.EncoderOps
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.utils.BootstrapStyles.Size
import org.scalajs.dom
import org.scalajs.dom.{Element, Event, HTMLInputElement, document}
import scribe.Logging
import ch.wsl.box.shared.utils.Formatters._

import scala.scalajs.js.URIUtils

case class ExportParams(metadata:JSONMetadata,selectedFields:Seq[JSONField],query:JSONQuery)



class ExportTableDialog extends Logging {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import ch.wsl.box.client.Context._
  import Implicits._


  val resolveFKId = "ExportResolveFk"
  val fieldSelectionClass = "exportFieldSelection"
  def fieldSelectors:Seq[HTMLInputElement] = modalBodyDiv.querySelectorAll(s".$fieldSelectionClass").map(_.asInstanceOf[HTMLInputElement]).toList

  val exportType = Property[ExportTableFormat](ExportTableFormat.XLS)
  val exportGeomFormat = Property[GeometryTableFormat](GeometryTableFormat.XY)
  var metadata:JSONMetadata = null
  var query:JSONQuery = null

  val modalBodyDiv = div().render

  def render(nested:Binding.NestedInterceptor, onOpen:() => ExportParams):Modifier = {

    val modal = popup(nested)

    document.getElementsByTagName("body").head.appendChild(div( position.absolute,top := 0, left := 0,
      modal
    ).render)

    Seq(
      button(`type` := "button", onclick :+= ((e:Event) => open(modal,onOpen())), ClientConf.style.boxButton, "Export"),

    )
  }

  private def download(format:ExportTableFormat) = {

    val kind = metadata.kind
    val modelName =  metadata.name

    val fields:Seq[String] = fieldSelectors.filter(_.checked).map(_.value)

    val resolveFK = if(document.getElementById(resolveFKId).asInstanceOf[HTMLInputElement].checked) {
      Seq(ExportTableFormat.fkParamName -> ExportMode.RESOLVE_FK)
    } else Seq()

    val queryNoLimits = query.copy(paging = None)

    val exportGeomFormatParam = metadata.geomFields.exists(f => fields.contains(f.name)) && format != ExportTableFormat.GeoPackage match {
      case true => Seq("exportGeomFormat" -> exportGeomFormat.get.toString)
      case false => Seq()
    }

    val params = Seq(
      ExportTableFormat.fieldsParamName -> fields.mkString(","),
      ExportTableFormat.queryParamName -> URIUtils.encodeURI(queryNoLimits.asJson.noSpaces)
    ) ++ resolveFK ++ exportGeomFormatParam

    val paramsString = params.map{ case (k,v) => s"$k=$v"}.mkString("&").replaceAll("\n", "")

    if(format == ExportTableFormat.PDF) {
      PDF.table(kind,modelName,fields,queryNoLimits)
    } else {
      val url = Routes.apiV1(
        s"/$kind/${services.clientSession.lang()}/$modelName/${format.code}?$paramsString"
      )
      logger.info(s"downloading: $url")
      dom.window.open(url)
    }
  }

  def open(modal:UdashModal,params:ExportParams) = {
    modalBodyDiv.innerHTML = ""
    metadata = params.metadata
    query = params.query
    modalBodyDiv.append(body(params))
    modal.show()
  }

  def body(params:ExportParams):Element = {



    div(maxWidth := 800.px,padding := 10.px,
      h3("Export - " + params.metadata.label),
      h5(marginTop := 15.px, "Format"),
      Select(
        exportType.bitransform(_.toString)(ExportTableFormat.fromString), ExportTableFormat.allSupported(params.metadata).map(_.toString).toSeqProperty
      )(Select.defaultLabel).render,
      h5(marginTop := 15.px,"Options"),
      div(
        div(width := 230.px, padding := 10.px, input(id := resolveFKId, `type` := "checkbox", checked)," Resolve Foreign Keys")
      ),
      div(float.right,marginTop := 10.px)(
        button("Select All", onclick :+= {(e:Event) =>
          fieldSelectors.foreach(_.checked = true)
        },ClientConf.style.boxButton),
        button("Unselect All", onclick :+= {(e:Event) =>
          fieldSelectors.foreach(_.checked = false)
        },ClientConf.style.boxButton)
      ),
      h5(marginTop := 15.px,"Select fields"),
      div(clear.both),
      div( display.flex, flexWrap.wrap,
        params.metadata.nativeFields.filterNot(_.`type` == JSONFieldTypes.GEOMETRY).sortBy(_.title).map{ f =>
          val _checked = params.selectedFields.contains(f)
          div(width := 230.px, padding := 10.px, input(`type` := "checkbox", `class` := fieldSelectionClass, value := f.name, checked.attrIf(_checked))," ",f.title)
        }
      ),
      if(params.metadata.geomFields.nonEmpty) {

          div(
            h5(marginTop := 15.px,"Geometry fields"),
            showIf(exportType.transform(_ != GeoPackage)) {
              div(
                p("Mode"),
                Select(
                  exportGeomFormat.bitransform(_.toString)(GeometryTableFormat.fromString), GeometryTableFormat.all.map(_.toString).toSeqProperty
                )(Select.defaultLabel).render
              ).render
            },
            div( display.flex, flexWrap.wrap,
              params.metadata.geomFields.sortBy(_.title).map{ f =>
                val _checked = params.selectedFields.contains(f)
                div(width := 230.px, padding := 10.px, input(`type` := "checkbox", `class` := fieldSelectionClass, value := f.name, checked)," ",f.title)
              }
            )
          )

      } else frag()
    ).render
  }





  private def popup(nested:Binding.NestedInterceptor) = {

    var modal:UdashModal = null

    val header = (x:NestedInterceptor) => div(
      b("Export"),
      UdashButton()( _ => Seq[Modifier](
        onclick :+= {(e:Event) => modal.hide(); e.preventDefault()},
        BootstrapStyles.close, "×"
      )).render
    ).render



    val body = (i:NestedInterceptor) => modalBodyDiv

    val footer = (x:NestedInterceptor) => div(
      button(onclick :+= ((e:Event) => {
        modal.hide()
        e.preventDefault()
      }), Labels.popup.close,ClientConf.style.boxButton),
      button(onclick :+= ((e:Event) => {
        download(exportType.get)
        modal.hide()
        e.preventDefault()
      }), "Export",ClientConf.style.boxButtonImportant)
    ).render

    modal = nested(UdashModal(modalSize = Some(Size.Small).toProperty)(
      headerFactory = Some(header),
      bodyFactory = Some(body),
      footerFactory = Some(footer)
    ))

    modal

  }

}
