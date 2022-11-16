package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.RoutingState
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.Navigate
import ch.wsl.box.client.views.components.widget.helpers.Link
import ch.wsl.box.client.views.components.widget.lookup.DynamicLookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.Event
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging

object LookupFormWidget extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.lookupForm

  override def create(params: WidgetParams): Widget = LookupFormWidgetImpl(params)

  case class LookupFormWidgetImpl(params: WidgetParams) extends Widget with Logging with Link {

    val field: JSONField = params.field

    val linked: LinkedForm = field.linked.get

    val linkedData: ReadableProperty[JSONID] = params.allData.transform { js =>
      val parentValues = linked.parentValueFields.map(k => js.js(k))
      JSONID.fromMap(linked.childValueFields.zip(parentValues))
    }

    def _params = params

    val lab = (linked.lookup,linked.label) match {
      case (Some(lookup),_) => new DynamicLookupWidget {
        override def params: WidgetParams = _params.copy(field = _params.field.copy(lookupLabel = Some(lookup)))

        override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = widget().showOnTable(nested)

        override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)


      }
      case (_,Some(label)) => Widget.forString(params.field,label)
      case (_,_) => Widget.forString(params.field,"Open")
    }


    def navigate(goTo: Routes => RoutingState) = (e: Event) => {
      Navigate.to(goTo(Routes(EntityKind.FORM.kind, linked.name)))
      e.preventDefault()
    }

    override protected def show(nested:Binding.NestedInterceptor): Modifier = nested(produce(linkedData) { case id =>
      linkRenderer(lab.render(false,Property(true),nested),field.params,navigate(_.show(id.asString))).render
    })

    override protected def edit(nested:Binding.NestedInterceptor): Modifier = nested(produce(linkedData) { case id =>
      linkRenderer(lab.render(false,Property(true),nested),field.params,navigate(_.edit(id.asString))).render
    })

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  nested(produce(linkedData) { case id =>
      a(onclick :+= navigate(_.show(id.asString)),lab.render(false,Property(true),nested)).render
    })

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  nested(produce(linkedData) { case id =>
      a(onclick :+= navigate(_.edit(id.asString)),lab.render(false,Property(true),nested)).render
    })
  }

}
