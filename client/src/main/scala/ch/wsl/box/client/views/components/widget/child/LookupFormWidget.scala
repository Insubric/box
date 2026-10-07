package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.{ContainerRoutingState, FinalRoutingState, PrefilledData, RoutingState}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.Navigate
import ch.wsl.box.client.views.components.widget.helpers.Link
import ch.wsl.box.client.views.components.widget.lookup.DynamicLookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.{EnhancedJson, FIRST}
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.utils.URLEncoder
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
      val parentValues = linked.fields(params.metadata).map{
        case LinkedParentField(k) => js.js(k)
        case LinkedParentStatic(s) => s
      }
      JSONID.fromMap(linked.childValueFields.zip(parentValues))
    }

    def insertNew = field.params.exists(_.js("new") == Json.True)

    def props: Option[Json] = for{
      fp <- field.params
      props <- fp.jsOpt("props")
    } yield props

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


    import ch.wsl.box.client.Context._

    def navigate(goTo: Routes => RoutingState) = (e: Event) => {
      val blank = params.field.params.exists(_.get("target") == "new_window")

      val state = goTo(Routes(linked.kind.kind, linked.name,params.public,params.popup,props))

      Navigate.to(state,blank)
      e.preventDefault()
    }

    override protected def show(nested:Binding.NestedInterceptor): Modifier = nested(produce(linkedData) { case id =>
      div(linkRenderer(lab.render(false,nested),field.params,navigate(_.show(id.asString)))).render
    })

    override protected def edit(nested:Binding.NestedInterceptor): Modifier = nested(produce(linkedData) { case id =>
      div(linkRenderer(lab.render(false,nested),field.params,navigate{ x =>
        if(insertNew)
          x.add() match {
            case state: PrefilledData => {
              val propsMap:Map[String,String] = props.toList.flatMap(_.asObject).flatMap(obj => obj.toList.map{case (k,v) => k -> v.string}).toMap
              val ifMap:Map[String,String] = id.id.toList.map(kv => kv.key -> kv.value.string).toMap
              state.withQueryParams(ifMap ++ propsMap)
            }
            case state => state
          }
        else
          x.edit(id.asString)
      })).render
    })

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  div(textAlign.center,show(nested))

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  div(textAlign.center,edit(nested))
  }

}
