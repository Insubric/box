package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.GeoJson.{Coordinates, Geometry, Point}
import ch.wsl.box.client.views.components.widget._
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.css.CssView._
import io.udash.properties.single.Property
import org.scalajs.dom
import org.scalajs.dom.Event
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol.projMod

import scala.scalajs.js


case class MapPointWidget(params: WidgetParams) extends Widget with MapWidget with HasData with Logging {


  import ch.wsl.box.client.utils.GeoJson.Geometry._

  override def field: JSONField = params.field

  override def data: Property[Json] = params.prop

  val geometry:Property[Option[Geometry]] = Property(None)

  val x:Property[String] = Property("")
  val y:Property[String] = Property("")

  val textPoint = x.combine(y){ case (x,y) =>
    s"lat: $y lng: $x"
  }

  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

  autoRelease(data.sync(geometry)(js => js.as[Geometry].toOption,point => point.asJson))

  autoRelease(geometry.sync(x)(
    {
      case Some(Point(coordinates)) => coordinates.x.toString
      case _ => ""
    },
    { txt =>
      txt.toDoubleOption.map{ x =>
        geometry.get match {
          case Some(Point(coordinates)) => Point(Coordinates(x,coordinates.y))
          case _ => Point(Coordinates(x,0))
        }
      }
    }
  ))
  autoRelease(geometry.sync(y)(
    {
      case Some(Point(coordinates)) => coordinates.y.toString
      case _ => ""
    },
    { txt =>
      txt.toDoubleOption.map{ y =>
        geometry.get match {
          case Some(Point(coordinates)) => Point(Coordinates(coordinates.x,y))
          case _ => Point(Coordinates(0,y))
        }
      }
    }
  ))

  override protected def show(): JsDom.all.Modifier = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      label(field.title),
      div(bind(textPoint)),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }

  override protected def edit(): JsDom.all.Modifier = {

    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    val modalStatus = Property(Status.Closed)

    var modal:UdashModal = null

    val header = (x:NestedInterceptor) => div(
      field.title,
      UdashButton()( _ => Seq[Modifier](
        onclick :+= ((e:Event) => modalStatus.set(Status.Closed)),
        BootstrapStyles.close, "×"
      )).render
    ).render

    val body = (x:NestedInterceptor) => {
      val mapOptions = options.copy(features = MapParamsFeatures(point = true,false,false,false,false,false,false))
      val mapParams = params.copy(field = field.copy(params = Some(mapOptions.asJson)))

      div(
        div(
          WidgetRegistry.forName(WidgetsNames.map).create(params = mapParams).render(true,Property(true))
        )
      ).render
    }

    val footer = (x:NestedInterceptor) => div(
      button(ClientConf.style.boxButton,onclick :+= ((e:Event) => {
        modal.hide()
        true
      }), Labels.form.save)
    ).render

    modal = UdashModal(modalSize = Some(Size.Large).toProperty)(
      headerFactory = Some(header),
      bodyFactory = Some(body),
      footerFactory = Some(footer)
    )

    modal.listen { case ev:ModalEvent =>
      ev.tpe match {
        case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
        case _ => {}
      }
    }

    modalStatus.listen{ state =>
      logger.info(s"State changed to:$state")
      state match {
        case Status.Open => modal.show()
        case Status.Closed => modal.hide()
      }
    }

    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      if(noLabel) frag() else WidgetUtils.toLabel(field)," ",
      div(
        display.`inline-block`,
        " Lat: ",NumberInput(y)(step := 0.00000000001,width := 70.px, float.none),
        " Lng: ",NumberInput(x)(step := 0.00000000001,width := 70.px, float.none)," ",
        WidgetUtils.addTooltip(Some("Get current coordinate with GPS"))(button(BootstrapStyles.Button.btn,backgroundColor := scalacss.internal.Color.transparent.value,paddingTop := 0.px, paddingBottom := 0.px)(
          onclick :+= ((e: Event) => dom.window.navigator.geolocation.getCurrentPosition{position =>
            val localCoords = projMod.transform(js.Array(position.coords.longitude,position.coords.latitude),wgs84Proj,defaultProjection)
            geometry.set(Some(Point(Coordinates(localCoords(0),localCoords(1)))))
          })
        )(Icons.target).render),
        WidgetUtils.addTooltip(Some("Show on map"))(button(BootstrapStyles.Button.btn,backgroundColor := scalacss.internal.Color.transparent.value, paddingTop := 0.px, paddingBottom := 0.px, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          true
        }),Icons.map).render),
        modal.render
      ),
      div(BootstrapStyles.Visibility.clearfix)
    )
  }
}

object MapPointWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.mapPoint

  override def create(params: WidgetParams): Widget = MapPointWidget(params)

}