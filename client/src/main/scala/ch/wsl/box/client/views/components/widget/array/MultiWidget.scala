package ch.wsl.box.client.views.components.widget.array

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.utils.ColorUtils.RGB
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.model.shared.JSONField._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import scalatags.JsDom
import io.circe.Json
import io.circe.syntax._
import io.udash.properties.single.Property
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._


object MultiWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.multi


  override def create(params: WidgetParams): Widget = MultiWidgetImpl(params)

  case class MultiWidgetImpl(params: WidgetParams) extends Widget {

    def toData(js:Json):Seq[(Json,Int)] = {
      val result = js.as[Seq[Json]].toOption.orElse{
        for {
          default <- field.default
          json <- io.circe.parser.parse(default).toOption
          seq <- json.as[Seq[Json]].toOption
        } yield seq

      }.getOrElse(Seq()).zipWithIndex
      result
    }

    def data:SeqProperty[(Json,Int)] = params.prop.bitransformToSeq[(Json,Int)](toData)(fw => Json.fromValues(fw.map(_._1)))

    def hasData:ReadableProperty[Boolean] = data.transform(_.nonEmpty)

    override def field: JSONField = params.field

    private def multiWidget = field.params.flatMap(_.getOpt("widget")).getOrElse(WidgetsNames.input)
    private def showTotal = field.`type` == JSONFieldTypes.ARRAY_NUMBER && field.params.exists(_.js("showTotal") == Json.True)
    private def showTotalLabel = WidgetUtils.i18nLabel(params.field.params,"showTotalLabel")
    private def totalValidate  = field.params.flatMap(_.js("totalValidate").as[Int].toOption)


    private val widgetJsonField = {
      field.params.flatMap(_.jsOpt("widgetFieldOpts")) match {
        case Some(value) => field.asJson.deepMerge(value).as[JSONField] match {
          case Left(value) => throw value
          case Right(value) => value
        }
        case None => field
      }
    }

    private def incrementalParams(i:Int) = widgetJsonField.params.map{ params =>
      params.deepMerge(params.seq("incrementalParams").lift(i).getOrElse(Json.fromFields(Seq())))
    }


    private def createWidget(d:Property[(Json,Int)]): Widget = {
      val i = d.get._2
      val prop = d.bitransform(_._1)((_,i))
      WidgetRegistry.forName(multiWidget).create(params.copy(prop = prop, field = widgetJsonField.copy(params = incrementalParams(i))))
    }

    private def add() = data.append((Json.Null,data.length))

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      div(
        nested(repeatWithNested(data) { (d, n) =>
          val widget = createWidget(d)
          widget.load()
          div(ClientConf.style.editableTableMulti, widget.editOnTable(n)).render
        }),
        if(showTotal) {
          nested(produce(data) { d =>
            val total = d.flatMap(_._1.as[Double].toOption).sum
            val valid = totalValidate.forall(_ == total)
            div(
              textAlign.center,fontWeight.bold,total,showTotalLabel,
              if(!valid) {
                Seq[Modifier](
                  input(required := "required", width := 1.px, height := 1.px, padding := 0, border := 0, float.left),
                  color := ClientConf.colorDanger
                )
              } else Seq[Modifier]()
            ).render
          })
        } else Seq[Modifier](),
        nested(showIf(data.transform(_.length < params.field.minMax.flatMap(_.max.map(_.toInt)).getOrElse(Int.MaxValue))) {
          a("Add", onclick :+= ((e: Event) => add())).render
        })
      )
    }

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        div(BootstrapStyles.Float.right(),bind(params.prop.transform(_.toString()))),
        tooltip(div(
          nested(repeat(data){d =>
            val widget = createWidget(d)
            div(widget.render(true,nested)).render
          })
        ).render)._1,
      )



    }
  }
}
