package ch.wsl.box.client.views.components.widget.array

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.utils.ColorUtils.RGB
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import scalatags.JsDom
import io.circe.Json
import io.circe.syntax._
import io.udash.properties.single.Property
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._
import typings.std.EventListener
import typings.toolcoolRangeSlider.mod.RangeSlider


object MultiWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.multi


  override def create(params: WidgetParams): Widget = MultiWidgetImpl(params)

  case class MultiWidgetImpl(params: WidgetParams) extends Widget {

    def data:SeqProperty[Json] = params.prop.bitransformToSeq[Json](o => o.as[Seq[Json]].getOrElse(Seq[Json]()))(fw => Json.fromValues(fw))

    override def field: JSONField = params.field

    def multiWidget = field.params.flatMap(_.getOpt("widget")).getOrElse(WidgetsNames.input)

    def createWidget(d:Property[Json]): Widget = WidgetRegistry.forName(multiWidget).create(params.copy(prop = d))

    def add() = data.append(Json.Null)

    override protected def show(): JsDom.all.Modifier = {}

    override def editOnTable(): JsDom.all.Modifier = {
      div(
        repeat(data){d =>
          div(ClientConf.style.editableTableMulti,createWidget(d).editOnTable()).render
        },
        a("Add", onclick :+= ((e:Event) => add()))
      )
    }

    override protected def edit(): JsDom.all.Modifier = {

      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field),
        div(BootstrapStyles.Float.right(),bind(params.prop.transform(_.toString()))),
        tooltip(div(
          repeat(data){d =>
            val widget = createWidget(d)
            div(widget.render(true,Property(true))).render
          }
        ).render)._1,
      )



    }
  }
}
