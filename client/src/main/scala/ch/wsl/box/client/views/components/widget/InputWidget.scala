package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import io.circe.Json
import io.circe.syntax._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.properties.single.Property
import scalatags.JsDom

import scala.concurrent.Future
import scalatags.JsDom.all._
import ch.wsl.box.shared.utils.JSONUtils._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.Node
import scribe.Logging

object InputWidgetFactory {

  object Input extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.input
    override def create(params: WidgetParams): Widget = new InputWidget.Input(params.field, params.prop)
  }

  object InputDisabled extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.inputDisabled
    override def create(params: WidgetParams): Widget = new InputWidget.TextDisabled(params.field, params.prop)
  }


  object TextArea extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.textarea
    override def create(params: WidgetParams): Widget = new InputWidget.Textarea(params.field, params.prop)

  }

  object TwoLines extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.twoLines
    override def create(params: WidgetParams): Widget = new InputWidget.TwoLines(params.field, params.prop)

  }

}

object InputWidget extends Logging {


  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._


  //used in read-only mode
  private def showMe(prop:Property[Json], field:JSONField, withLabel:Boolean, modifiers:Seq[Modifier] = Seq()):Binding = WidgetUtils.showNotNull(prop){ p =>

    val inputRendererDefaultModifiers:Seq[Modifier] = Seq(BootstrapStyles.Float.right())

    def reallyWithLabel = withLabel & (field.title.length > 0)

    val mods = if(reallyWithLabel)
      inputRendererDefaultModifiers++modifiers
    else
      inputRendererDefaultModifiers++modifiers++Seq(width := 100.pct)



    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      if(reallyWithLabel) label(field.title) else {},
      div(`class` := TestHooks.readOnlyField(field.name) ,mods, bind(prop.transform(_.string))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render

  }

  private def editMe(field:JSONField, withLabel:Boolean, skipRequiredInfo:Boolean=false, modifiers:Seq[Modifier] = Seq())(inputRenderer:(Seq[Modifier]) => Node):Modifier = {

    val inputRendererDefaultModifiers:Seq[Modifier] = Seq(BootstrapStyles.Float.right())

    def reallyWithLabel = withLabel & (field.title.length > 0)

    val ph = field.placeholder match{
      case Some(p) if p.nonEmpty => Seq(placeholder := p)
      case _ => Seq.empty
    }

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _


    val allModifiers:Seq[Modifier] =  inputRendererDefaultModifiers++
                        ph ++
                        WidgetUtils.toNullable(field.nullable) ++
                        Seq(`class` := TestHooks.formField(field.name)) ++
                        modifiers

    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      if(reallyWithLabel) WidgetUtils.toLabel(field, skipRequiredInfo) else {},
      if(reallyWithLabel)
        tooltip(inputRenderer(allModifiers))
      else
        tooltip(inputRenderer(allModifiers++Seq(width := 100.pct))),
      div(BootstrapStyles.Visibility.clearfix)
    )

  }


  class TextDisabled(field:JSONField, data: Property[Json]) extends Input(field,data) {

    override val modifiers = Seq(disabled := true, textAlign.right)
  }



  class Textarea(val field:JSONField, val data: Property[Json]) extends Widget with HasData {

    val modifiers:Seq[Modifier] = Seq()

    override def edit() = editMe(field,true, false, modifiers){ case y =>
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(jsonToString _,strToJson(field.nullable) _))
      val mod = y ++ WidgetUtils.toNullable(field.nullable)
      TextArea(stringModel)(mod:_*).render
    }
    override protected def show(): JsDom.all.Modifier = autoRelease(showMe(data,field,true,modifiers))
  }

  class TwoLines(field:JSONField, prop: Property[Json]) extends Textarea(field,prop) {

    override val modifiers: Seq[JsDom.all.Modifier] = Seq(rows := 2)
  }


  class Input(val field:JSONField, val data: Property[Json]) extends Widget with HasData {

    val modifiers:Seq[Modifier] = Seq()

    val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

    def fromString(s:String) = field.`type` match {
      case JSONFieldTypes.NUMBER => strToNumericJson(s)
      case JSONFieldTypes.ARRAY_NUMBER => strToNumericArrayJson(s)
      case _ => strToJson(field.nullable)(s)
    }

    override def edit():JsDom.all.Modifier = (editMe(field, !noLabel, false){ case y =>
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(jsonToString _,fromString _))
      field.`type` match {
        case JSONFieldTypes.NUMBER => NumberInput(stringModel)(y:_*).render
        case JSONFieldTypes.ARRAY_NUMBER => NumberInput(stringModel)(y++modifiers:_*).render
        case _ => TextInput(stringModel)(y++modifiers:_*).render
      }
    })
    override protected def show(): JsDom.all.Modifier = autoRelease(showMe(data, field, !noLabel))


    override def editOnTable(): JsDom.all.Modifier = {
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(jsonToString _,fromString _))
      TextInput(stringModel)(ClientConf.style.simpleInput).render
    }
  }

}

