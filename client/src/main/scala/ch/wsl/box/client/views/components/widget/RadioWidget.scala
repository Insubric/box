package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.model.shared.Internationalization.I18n
import ch.wsl.box.model.shared.{Internationalization, JSONField, WidgetsNames}
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.bootstrap.utils.BootstrapStyles.Form
import org.scalajs.dom.html.Input
import scalatags.JsDom


object RadioWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.radio

  override def create(params: WidgetParams): Widget = RadioWidgetImpl(params)

  case class RadioWidgetImpl(params: WidgetParams) extends Widget with HasData {

    import io.udash.css.CssView._
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._
    import ch.wsl.box.client.Context.Implicits._
    import ch.wsl.box.shared.utils.JSONUtils._
    import Internationalization._

    override def field: JSONField = params.field

    case class OptionEntry(label: Either[String,I18n],value:Json)
    import io.circe.generic.auto._

    val options:Option[Seq[OptionEntry]] = field.params.flatMap(_.jsOpt("options")).flatMap{ p =>

      p.as[Seq[OptionEntry]] match {
        case Left(value) => {
          println(value.toString())
          p.asArray.map{ _.map(r => OptionEntry(Left(r.string),r) ) }
        }
        case Right(value) => Some(value)
      }

    }

    import ch.wsl.box.client.Context._

    def translatedLabel(x:OptionEntry) = Internationalization.either(services.clientSession.lang())(x.label)

    def jsToString(js:Json):String = options.flatMap(_.find(_.value == js)).flatMap(translatedLabel).getOrElse("")
    def stringToJs(s:String):Json = options.flatMap(_.find(x => translatedLabel(x).contains(s))).map(_.value).getOrElse(Json.Null)

    def includeNullOptions = options.map(Seq(OptionEntry(Left(""),Json.Null)) ++ _ )

    override def edit(nested:Binding.NestedInterceptor) = {
      includeNullOptions match {
        case Some(opt) => editWithOptions(opt,nested)
        case None => div( "Error: options not defined in params" )
      }
    }

    private def editWithOptions(options:Seq[OptionEntry], nested:Binding.NestedInterceptor) = {
      val tooltip = WidgetUtils.addTooltip(field.tooltip,UdashTooltip.Placement.Right) _


      val m:Seq[Modifier] = Seq[Modifier](width.auto,margin := 5.px)

      val stringProp = Property("")

      autoRelease(data.sync[String](stringProp)(js => jsToString(js),s => stringToJs(s)))


      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        tooltip({
          div(BootstrapStyles.Float.right(),
            RadioButtons[String](stringProp, options.flatMap(translatedLabel).toSeqProperty)(
              els => span(els.map {
                case (_, "") => frag()
                case (i: Input, l: String) => {
                  i.setAttribute("name",field.name)
                  if(!field.nullable) i.setAttribute("required","required")
                  label(Form.checkInline)(i, l)
                }
              },m).render
            )
          )
        }.render)._1,
        div(BootstrapStyles.Visibility.clearfix)
      )
    }

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.none(),width.auto,margin := 5.px) ++ WidgetUtils.toNullable(field.nullable)
      val stringProp = Property("")

      autoRelease(data.sync[String](stringProp)(js => jsToString(js),s => stringToJs(s)))



      RadioButtons(stringProp, includeNullOptions.getOrElse(Seq()).flatMap(translatedLabel).toSeqProperty)(
        els => span(els.map {
          case (_, "") => frag()
          case (i: Input, l: String) => label(Form.checkInline)(i, l)
        },ClientConf.style.distributionContrainer).render,m
      ).render

    }

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = WidgetUtils.showNotNull(data,nested) { p =>
      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12), ClientConf.style.noPadding, ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field, WidgetUtils.LabelRight),
        div(BootstrapStyles.Float.right(), bind(params.prop.transform(_.toString()))),
        tooltip(div(nested(bind(data.transform(_.string)))).render)._1,
      ).render
    }
    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = WidgetUtils.showNotNull(data,nested) { p =>
      div(
        nested(bind(data.transform(_.string)))
      ).render
    }


    override def data: Property[Json] = params.prop
  }
}
