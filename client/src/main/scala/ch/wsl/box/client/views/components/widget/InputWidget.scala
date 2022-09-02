package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONMetadata, WidgetsNames}
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
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import org.scalajs.dom.{Event, HTMLInputElement, HTMLTextAreaElement, Node, document}
import scribe.Logging

import java.util.UUID

object InputWidgetFactory {

  object Input extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.input
    override def create(params: WidgetParams): Widget = new InputWidget.Input(params.field, params.prop)
  }

  object IntegerDecimal2 extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.integerDecimal2
    override def create(params: WidgetParams): Widget = new InputWidget.IntegerDecimal2(params.field, params.prop)
  }

  object InputDisabled extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.inputDisabled
    override def create(params: WidgetParams): Widget = new InputWidget.TextDisabled(params.field, params.prop)
  }


  object TextArea extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.textarea
    override def create(params: WidgetParams): Widget = new InputWidget.Textarea(params.field, params.prop,params.metadata)

  }

  object TwoLines extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.twoLines
    override def create(params: WidgetParams): Widget = new InputWidget.TwoLines(params.field, params.prop,params.metadata)

  }

}

object InputWidget extends Logging {


  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._


  //used in read-only mode
  private def showMe(prop:ReadableProperty[Json], field:JSONField, withLabel:Boolean, modifiers:Seq[Modifier] = Seq()):Binding = WidgetUtils.showNotNull(prop){ p =>

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
        tooltip(inputRenderer(allModifiers))._1
      else
        tooltip(inputRenderer(allModifiers++Seq(width := 100.pct)))._1,
      div(BootstrapStyles.Visibility.clearfix)
    )

  }


  class TextDisabled(field:JSONField, data: Property[Json]) extends Input(field,data) {

    override val modifiers = Seq(disabled := true, textAlign.right)
  }



  class Textarea(val field:JSONField, val data: Property[Json], metadata:JSONMetadata) extends Widget with HasData {

    val modifiers:Seq[Modifier] = Seq()

    override def edit() = editMe(field,true, false, modifiers){ case y =>
      val stringModel = Property("")
      val textAreaId = UUID.randomUUID().toString
      stringModel.listen{_ =>
        val el = document.getElementById(textAreaId).asInstanceOf[HTMLTextAreaElement]
        el.style.height = if(el.scrollHeight > el.clientHeight)  el.scrollHeight+"px" else "30px";
      }
      autoRelease(data.sync[String](stringModel)(jsonToString _,strToJson(field.nullable) _))
      val mod = y ++ WidgetUtils.toNullable(field.nullable) ++ Seq(id := textAreaId)
      TextArea(stringModel)(mod:_*).render
    }
    override protected def show(): JsDom.all.Modifier = autoRelease(showMe(data,field,true,modifiers))

    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    val modalStatus = Property(Status.Closed)

    var modal:UdashModal = null

    val header = (x:NestedInterceptor) => div(
      b(field.title),
      div(width := 100.pct, textAlign.center,field.title),
      UdashButton()( _ => Seq[Modifier](
        onclick :+= {(e:Event) => modalStatus.set(Status.Closed); e.preventDefault()},
        BootstrapStyles.close, "×"
      )).render
    ).render

    val textAreaId = TestHooks.popupField(field.name,metadata.objId)

    val body = (x:NestedInterceptor) => {
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(jsonToString _,strToJson(field.nullable) _))
      div(
        div(
          TextArea(stringModel)(WidgetUtils.toNullable(field.nullable), width := 100.pct, height := 300.px, id := textAreaId)
        )
      ).render
    }

    val footer = (x:NestedInterceptor) => div(
      button(onclick :+= ((e:Event) => {
        modal.hide()
        e.preventDefault()
      }), Labels.popup.close,ClientConf.style.boxButton)
    ).render

    modal = UdashModal(modalSize = Some(Size.Large).toProperty)(
      headerFactory = Some(header),
      bodyFactory = Some(body),
      footerFactory = Some(footer)
    )

    modal.listen { case ev:ModalEvent =>
      ev.tpe match {
        case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
        case ModalEvent.EventType.Shown => document.getElementById(textAreaId).asInstanceOf[HTMLTextAreaElement].focus()
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

    override def editOnTable(): JsDom.all.Modifier = {
      div(
        bind(data.transform{ str =>
          if(str.string.length > 40) {
            str.string.take(37) + "..."
          } else if(str.isString) str.string else ""
        }),
        " ",
        a(fontSize := 20.px, "✎",onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        })),
        modal.render
      )
    }

  }

  class TwoLines(field:JSONField, prop: Property[Json], metadata:JSONMetadata) extends Textarea(field,prop,metadata) {

    override val modifiers: Seq[JsDom.all.Modifier] = Seq(rows := 2)
  }


  class Input(val field:JSONField, val data: Property[Json]) extends Widget with HasData {

    val modifiers:Seq[Modifier] = Seq()

    val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

    def fromString(s:String) = field.`type` match {
      case JSONFieldTypes.NUMBER | JSONFieldTypes.INTEGER => strToNumericJson(s)
      case JSONFieldTypes.ARRAY_NUMBER => strToNumericArrayJson(s)
      case _ => strToJson(field.nullable)(s)
    }

    override def edit():JsDom.all.Modifier = (editMe(field, !noLabel, false){ case y =>
      val stringModel = Property("")

      data.sync[String](stringModel)(jsonToString _,fromString _)

      data.listen(prop => println(s"Input property change to: $prop"))
      stringModel.listen(prop => println(s"String model property change to: $prop"))

      if(TestHooks.testing) {
        TestHooks.properties += TestHooks.formField(field.name) -> data
      }

      field.`type` match {
        case JSONFieldTypes.NUMBER => NumberInput(stringModel)((y ++ Seq(step := "any")):_*).render
        case JSONFieldTypes.INTEGER => NumberInput(stringModel)(y:_*).render
        case JSONFieldTypes.ARRAY_NUMBER => NumberInput(stringModel)(y++modifiers:_*).render
        case _ => TextInput(stringModel)(y++modifiers:_*).render
      }
    })
    override protected def show(): JsDom.all.Modifier = autoRelease(showMe(data, field, !noLabel))


    override def editOnTable(): JsDom.all.Modifier = {
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(jsonToString _,fromString _))
      val mod:Seq[Modifier] = Seq[Modifier](ClientConf.style.simpleInput) ++ WidgetUtils.toNullable(field.nullable)
      TextInput(stringModel)(mod:_*).render
    }
  }

  class IntegerDecimal2(val field:JSONField, val data: Property[Json]) extends Widget with HasData {

    val modifiers:Seq[Modifier] = Seq()

    val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

    def fromString(s:String) =   s match {
      case "" => Json.Null
      case _ => (s.toDouble * 100).toInt.asJson
    }

    def toString(js:Json):String = {
      js.as[Int] match {
        case Left(_) if js == Json.Null => ""
        case Left(_) => {
          logger.error(s"$js is not an integer")
          ""
        }
        case Right(value) =>{
          "%.2f".format(value / 100.0)
        }
      }
    }

    override def edit():JsDom.all.Modifier = (editMe(field, !noLabel, false){ case y =>
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(toString _,fromString _))
      NumberInput(stringModel)((y ++ Seq(step := "0.01")):_*).render
    })

    override protected def show(): JsDom.all.Modifier = autoRelease(showMe(data.transform{ js =>
      if(js.isNumber) {
        js.as[Double].toOption.map(x => "%.2f".format(x / 100.0).asJson).getOrElse(Json.Null)
      } else js
    }, field, !noLabel))


    override def editOnTable(): JsDom.all.Modifier = {
      val stringModel = Property("")
      autoRelease(data.sync[String](stringModel)(toString _,fromString _))
      NumberInput(stringModel)(ClientConf.style.simpleInput,step := "0.01").render
    }

    override def showOnTable(): JsDom.all.Modifier = autoRelease(bind(data.transform{ js =>
      if(js.isNumber) {
        js.as[Double].toOption.map(x => "%.2f".format(x / 100.0)).getOrElse("")
      } else ""
    }))

  }

}

