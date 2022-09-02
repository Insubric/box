package ch.wsl.box.client.views

/**
  * Created by andre on 4/3/2017.
  */

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels, Navigate, REST, UI}
import ch.wsl.box.client.styles.{BootstrapCol}
import ch.wsl.box.client.{EntitiesState, EntityFormState, EntityTableState}
import ch.wsl.box.model.shared.EntityKind
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.form.UdashForm
import io.udash.core.Presenter
import org.scalajs.dom.{Element, Event}
import scalatags.generic

case class Entities(list:Seq[String], currentEntity:Option[String], kind:Option[String], search:String, filteredList:Seq[String])
object Entities extends HasModelPropertyCreator[Entities] {
  implicit val blank: Blank[Entities] =
    Blank.Simple(Entities(Seq(),None,None,"",Seq()))
}

case class EntitiesViewPresenter(kind:String, modelName:String, sidebarWidth:Int) extends ViewFactory[EntitiesState] {



  override def create(): (View, Presenter[EntitiesState]) = {
    val model = ModelProperty.blank[Entities]
    val routes = Routes(kind,modelName)
    val presenter = new EntitiesPresenter(model)
    val view = new EntitiesView(model,presenter,sidebarWidth,routes)
    (view,presenter)
  }
}

class EntitiesPresenter(model:ModelProperty[Entities]) extends Presenter[EntitiesState] {


  import ch.wsl.box.client.Context._

  override def handleState(state: EntitiesState): Unit = {
    model.subProp(_.kind).set(Some(state.kind))
    if(services.clientSession.logged.get) {
      services.rest.entities(new EntityKind(state.kind).entityOrForm).map { models =>
        model.subSeq(_.list).set(models)
        model.subSeq(_.filteredList).set(models)
      }
    }

    if(state.currentEntity != "") {
      model.subProp(_.currentEntity).set(Some(state.currentEntity))
    } else {
      model.subProp(_.currentEntity).set(None)
    }
  }

  model.subProp(_.search).listen{ search =>
    model.subProp(_.filteredList).set(model.subProp(_.list).get.filter(m => m.contains(search)))
  }


}

class EntitiesView(model:ModelProperty[Entities], presenter: EntitiesPresenter, sidebarWidth:Int, routes:Routes) extends ContainerView {
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._


  val sidebarGrid = BootstrapCol.md(sidebarWidth)
  def contentGrid = if(UI.showEntitiesSidebar) BootstrapCol.md(12-sidebarWidth) else BootstrapCol.md(12)

  override def renderChild(view: Option[View]): Unit = {

    import io.udash.wrappers.jquery._
    jQ(content).children().remove()
    if(view.isDefined) {
      view.get.getTemplate.applyTo(content)
    }

  }


  private val content: Element = div().render

  private def sidebar: Element = if(UI.showEntitiesSidebar) {
    div(sidebarGrid)(
      showIf(model.subProp(_.currentEntity).transform(_.isDefined)) {
        div(
          div(Labels.entities.search),
          TextInput(model.subProp(_.search))(),
          ul(ClientConf.style.noBullet,
            repeat(model.subSeq(_.filteredList)) { m =>
              li(a(Navigate.click(routes.entity(m.get)), m.get)).render
            }
          )

        ).render
      }
    ).render
  } else div().render

  override def getTemplate: scalatags.generic.Modifier[Element] = div(BootstrapStyles.Grid.row)(
    sidebar,
    div(contentGrid)(
      produce(model.subProp(_.currentEntity))( ce =>
        ce match {
          case None => div(
            h1(Labels.entities.title),
            p(Labels.entities.select),
            div(Labels.entities.search),
            TextInput(model.subProp(_.search))(),
            ul(ClientConf.style.noBullet,
              repeat(model.subSeq(_.filteredList)){m =>
                li(a(Navigate.click(routes.entity(m.get)),m.get)).render
              }
            )

          ).render
          case Some(model) => div().render
        }
      ),
      content
    )
  )
}
