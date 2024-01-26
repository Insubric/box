package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.RoutingState
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Navigate}
import ch.wsl.box.client.views.components.widget.helpers.Link
import ch.wsl.box.client.views.components.widget.lookup.DynamicLookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.generic.auto._
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.Event
import scalacss.internal.Color
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging

import scala.concurrent.Future

object LinkedFormWidget extends ComponentWidgetFactory {



  override def name: String = WidgetsNames.linkedForm

  override def create(params: WidgetParams): Widget = LinkedFormWidgetImpl(params)

  case class LinkedFormWidgetImpl(params: WidgetParams) extends Widget with Logging with Link {

    import ch.wsl.box.client.Context._
    import ch.wsl.box.client.Context.Implicits._
    import io.udash.css.CssView._
    import scalacss.ScalatagsCss._


    val field: JSONField = params.field

    val linkedFormName = field.linked.map(_.name).getOrElse("unknown")

    def navigate(goTo: Routes => RoutingState) =  Navigate.to(goTo(Routes(EntityKind.FORM.kind, linkedFormName)))

    val label = field.label.orElse(field.linked.flatMap(_.label)).orElse(field.linked.map(_.name)).getOrElse("Open")

    def loadAndGo(edit:Boolean,directToForm:Seq[String] => Boolean) = {
      val query = field.query.getOrElse(JSONQuery.empty).withData(params.allData.get,services.clientSession.lang())
      services.rest.ids(EntityKind.FORM.kind, services.clientSession.lang(), linkedFormName, query).map { ids =>
        services.clientSession.setIDs(ids)
        if(directToForm(ids.ids)) {
          if (edit) {
            ids.ids.headOption match {
              case Some(value) => navigate(_.edit(value))
              case None => navigate(_.add())
            }
          } else {
            navigate(_.show(ids.ids.headOption.getOrElse("")))
          }
        } else {
          navigate(_.entity(field.query))
        }
      }
    }

    def goto(edit:Boolean):Event => Any = (e: Event) => {
      e.preventDefault()
      field.params.map(_.get("open")) match {
        case Some("first") => loadAndGo(edit,_ => true)
        case Some("listOrSingle") => loadAndGo(edit,_.length <= 1)
        case Some("new") => {
          if(edit) {
            navigate(_.add())
          }
        }
        case _ => Future.successful(navigate(_.entity(field.query)))
      }
    }

    override protected def show(nested:Binding.NestedInterceptor): Modifier = linkRenderer(label,field.params,goto(false))

    override protected def edit(nested:Binding.NestedInterceptor): Modifier = linkRenderer(label,field.params,goto(true))

    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested) //a(onclick :+= goto(false),label)

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = edit(nested) //a(onclick :+= goto(true),label)
  }

}
