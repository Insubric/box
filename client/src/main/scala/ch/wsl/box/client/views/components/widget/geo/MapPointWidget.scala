package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.geo.{BoxMapProjections, MapParams, MapParamsFeatures}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.GPS
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Geometry, Point}
import ch.wsl.box.client.views.components.widget._
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.css.CssView._
import io.udash.properties.single.Property
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLInputElement, document, window}
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol.projMod

import java.util.UUID
import scala.scalajs.js


case class MapPointWidget(params: WidgetParams) extends Widget with HasData with Logging {



  import ch.wsl.box.model.shared.GeoJson.Geometry._
  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  val options: MapParams = MapWidgetUtils.options(field)
  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)

  override def field: JSONField = params.field

  override def data: Property[Json] = params.prop

  val geometry:Property[Option[Geometry]] = Property(None)

  val x:Property[String] = Property("")
  val y:Property[String] = Property("")

  val noLabel = field.params.exists(_.js("nolabel") == Json.True)
  val useXY = field.params.exists(_.js("useXY") == Json.True)
  val coordinateLabel = field.params.flatMap(_.getOpt("coordinateLabel")).map(x => s"[$x]").getOrElse("")

  val textPoint = x.combine(y){ case (x,y) =>
    useXY match {
      case true => s"x: $x y: $y $coordinateLabel"
      case false => s"lat: $y lng: $x $coordinateLabel"
    }

  }



  autoRelease(data.sync(geometry)(js => js.as[Geometry].toOption,point => point.asJson))

  val xId = s"x-field-${UUID.randomUUID()}"
  val yId = s"y-field-${UUID.randomUUID()}"

  def xHtmlElement = document.getElementById(xId).asInstanceOf[HTMLInputElement]
  def yHtmlElement = document.getElementById(yId).asInstanceOf[HTMLInputElement]

  def checkValidity(x:String,y:String):Unit = {
    (x.toDoubleOption,y.toDoubleOption) match {
      case (Some(x),None) => { //show Y error
        xHtmlElement.setCustomValidity("")
        yHtmlElement.setCustomValidity("Y not valid")
      }
      case (None,Some(y)) => { // show X error
        xHtmlElement.setCustomValidity("X not valid")
        yHtmlElement.setCustomValidity("")
      }
      case _ => {
        xHtmlElement.setCustomValidity("")
        yHtmlElement.setCustomValidity("")
      } // all good
    }
    window.setTimeout(() => { // wait for the focus to be correctly set
      if(document.activeElement.id != xId) // only check if is not the current field
        xHtmlElement.checkValidity()
      if(document.activeElement.id != yId)
        yHtmlElement.checkValidity()
    },0)
  }

  autoRelease(geometry.sync(x)(
    {
      case Some(Point(coordinates,crs)) => coordinates.x.toString
      case _ => x.get
    },
    { txt =>
      (txt.toDoubleOption,y.get.toDoubleOption) match {
        case (Some(x),Some(y)) => Some(Point(Coordinates(x,y),options.crs))
        case _ => None
      }
    }
  ))
  autoRelease(geometry.sync(y)(
    {
      case Some(Point(coordinates,crs)) => coordinates.y.toString
      case _ => y.get
    },
    { txt =>
      (txt.toDoubleOption,x.get.toDoubleOption) match {
        case (Some(y),Some(x)) => Some(Point(Coordinates(x,y),options.crs))
        case _ => None
      }
    }
  ))

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      label(field.title),
      div(bind(textPoint)),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }

  object Status {
    val Closed = "closed"
    val Open = "open"
  }

  private def mapModal(nested: NestedInterceptor): (UdashModal, CastableProperty[String]) = {


    val modalStatus = Property(Status.Closed)

    var modal: UdashModal = null

    val header = (x: NestedInterceptor) => div(
      field.title,
      UdashButton()(_ => Seq[Modifier](
        onclick :+= { (e: Event) => modalStatus.set(Status.Closed); e.preventDefault() },
        BootstrapStyles.close, "×"
      )).render
    ).render

    val body = (x: NestedInterceptor) => {
      val mapOptions = options.copy(features = MapParamsFeatures(point = true, false, false, false, false, false, false))
      val mapParams = params.copy(field = field.copy(params = Some(mapOptions.asJson)))

      div(
        div(
          WidgetRegistry.forName(WidgetsNames.map).create(params = mapParams).render(true, nested)
        )
      ).render
    }

    val footer = (x: NestedInterceptor) => div(
      button(ClientConf.style.boxButton, onclick :+= ((e: Event) => {
        modal.hide()
        e.preventDefault()
      }), Labels.form.save)
    ).render

    modal = UdashModal(modalSize = Some(Size.Large).toProperty)(
      headerFactory = Some(header),
      bodyFactory = Some(body),
      footerFactory = Some(footer)
    )

    modal.listen { case ev: ModalEvent =>
      ev.tpe match {
        case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
        case _ => {}
      }
    }

    modalStatus.listen { state =>
      logger.info(s"State changed to:$state")
      state match {
        case Status.Open => modal.show()
        case Status.Closed => modal.hide()
      }
    }

    (modal,modalStatus)

  }

  private def xInput(mod:Modifier*) = NumberInput(x)(
    id := xId,
    step := 0.00000000001,
    float.none,
    WidgetUtils.toNullable(field.nullable),
    onblur :+= ((e:Event) => checkValidity(x.get,y.get)),
    onkeydown :+= WidgetUtils.stopEnterUpDownEventHandler,
    mod
  )
  private def yInput(mod:Modifier*) = NumberInput(y)(
    id := yId,
    step := 0.00000000001,
    float.none,
    WidgetUtils.toNullable(field.nullable),
    onblur :+= ((e:Event) => checkValidity(x.get,y.get)),
    onkeydown :+= WidgetUtils.stopEnterUpDownEventHandler,
    mod
  )

  private def gpsButton(mod:Modifier*) = WidgetUtils.addTooltip(Some("Get current coordinate with GPS"))(a(BootstrapStyles.Button.btn, backgroundColor := scalacss.internal.Color.transparent.value, paddingTop := 0.px, paddingBottom := 0.px)(
    onclick :+= { (e: Event) =>
      GPS.coordinates().map { coords =>
        val point = coords.map { c =>
          val localCoords = projMod.transform(js.Array(c.x, c.y), proj.wgs84Proj, proj.defaultProjection)
          Point(Coordinates(localCoords(0), localCoords(1)),options.crs)
        }
        geometry.set(point)
      }
      e.preventDefault() // needed in order to avoid triggering the form validation
    }
  )(Icons.target).render)._1

  private def showMap(modalStatus:Property[String],mod:Modifier*) = {
    WidgetUtils.addTooltip(Some("Show on map"))(a(BootstrapStyles.Button.btn, backgroundColor := scalacss.internal.Color.transparent.value, paddingTop := 0.px, paddingBottom := 0.px, onclick :+= ((e: Event) => {
      modalStatus.set(Status.Open)
      e.preventDefault()
    }), Icons.map).render)._1
  }

  override def editOnTable(nested: NestedInterceptor): JsDom.all.Modifier = {

    val (modal,modalStatus) = mapModal(nested)


    div(ClientConf.style.flexContainer,
        useXY match {
          case false => Seq[Modifier](
            yInput(placeholder := s"Lat" ),xInput(placeholder := "Lng")
          )
          case true => Seq[Modifier](
            xInput(placeholder := s"x"),yInput(placeholder := "y")
          )
        },
        gpsButton(),
        showMap(modalStatus),
        modal.render

    )
  }

  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val (modal,modalStatus) = mapModal(nested)

    val xIn = xInput(width := 70.px)
    val yIn = yInput(width := 70.px)

    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      div(ClientConf.style.label50,if(noLabel) frag() else WidgetUtils.toLabel(field,WidgetUtils.LabelRight)),
      div(
        display.`inline-block`,
        useXY match {
          case false => Seq[Modifier](
            s"$coordinateLabel Lat: ",yIn,
            " Lng: ",xIn," "
          )
          case true => Seq[Modifier](
            s"$coordinateLabel x: ",xIn,
            s" y: ",yIn," "
          )
        },
        gpsButton(),
        showMap(modalStatus),
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