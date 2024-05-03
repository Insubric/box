package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import scalatags.JsDom
import io.circe.Json
import io.circe.syntax._
import io.circe.scalajs._
import io.udash.properties.single.Property
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._
import ch.wsl.typings.choicesJs.anon.PartialOptions
import ch.wsl.typings.choicesJs.publicTypesSrcScriptsInterfacesChoiceMod.Choice
import ch.wsl.typings.choicesJs.mod

import scala.scalajs.js
import js.JSConverters._
import scala.scalajs.js.|


object MultipleLookupWidget extends ComponentWidgetFactory  {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.multipleLookup


  override def create(params: WidgetParams): Widget = MultipleLookupWidgetImpl(params)

  case class MultipleLookupWidgetImpl(params: WidgetParams) extends  LookupWidget {


    override def allData: ReadableProperty[Json] = params.allData

    override def public: Boolean = params.public

    override def metadata: JSONMetadata = params.metadata

    override def data: Property[Json] = params.prop

    override def field: JSONField = params.field

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      nested(produce(params.prop.combine(lookup)((x, y) => (x, y))) { case (d, lookups) =>
        d.asArray match {
          case Some(value) => {
            val items: Seq[Modifier] = value.map { id =>
              val label: String = lookups.find(_.id == id).map(_.value).getOrElse(id.string)
              div(label)
            }
            span(items).render
          }
          case None => span().render
        }

      })
    }

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)

    override def editOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = {
      multipleSelect
    }

    // TODO still buggy
    def multipleSelect = {
      val el = select().render


      val observer = new MutationObserver({(mutations,observer) =>
        if(document.contains(el)) {
          observer.disconnect()
          val options = PartialOptions()
          val items = params.prop.get.as[Seq[String]] match {
            case Left(value) => Seq[Choice | String]().toJSArray
            case Right(value) => value.asInstanceOf[Seq[Choice | String]].toJSArray
          }

          options.setItems(items)
          val choicesJs = new mod.default(el,options)
          el.addEventListener("change",(e:Event) => {
            (choicesJs.getValue(true):Any) match {
              case list: js.Array[String] => params.prop.set(list.asJson)
              case a: String => params.prop.set(Seq(a).asJson)
            }
          })

          lookup.listen(values => {
            val choices = values.toSeq.map(x => Choice(x.value,x.id.string)).toJSArray.asInstanceOf[js.Array[Choice | ch.wsl.typings.choicesJs.publicTypesSrcScriptsInterfacesGroupMod.Group]]
            choicesJs.setChoices(choices)
          },true)
        }
      })

      observer.observe(document,MutationObserverInit(childList = true, subtree = true))
      el
    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {


      val tooltip = WidgetUtils.addTooltip(field.tooltip) _


      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        tooltip(multipleSelect)._1,
      )

    }
  }
}
