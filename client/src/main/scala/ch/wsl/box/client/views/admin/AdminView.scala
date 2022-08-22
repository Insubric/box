package ch.wsl.box.client.views.admin

import ch.wsl.box.client._
import ch.wsl.box.client.services.{ClientConf, Navigate}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.model.shared._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.Event
import scalacss.ScalatagsCss._

case class AdminViewModel(entities:Seq[String])
object AdminViewModel extends HasModelPropertyCreator[AdminViewModel] {
  implicit val blank: Blank[AdminViewModel] =
    Blank.Simple(AdminViewModel(Seq()))
}

object AdminViewPresenter extends ViewFactory[AdminState.type]{

  val prop = ModelProperty.blank[AdminViewModel]

  override def create() = {
    val presenter = new AdminPresenter(prop)
    (new AdminView(prop,presenter),presenter)
  }
}

class AdminPresenter(viewModel:ModelProperty[AdminViewModel]) extends Presenter[AdminState.type] {

  import Context._

  override def handleState(state: AdminState.type): Unit = {
    for{
      entitites <- services.rest.entities(EntityKind.ENTITY.kind)
    } yield {
      viewModel.set(AdminViewModel(entitites))
    }
  }

  def generateStub(entity: => String) = (e:Event) => {
    services.rest.generateStub(entity)
    e.preventDefault()
  }
}

class AdminView(viewModel:ModelProperty[AdminViewModel], presenter:AdminPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._

  private val entityForStub = Property("")

  private val sourceLang = Property(ClientConf.langs.headOption.getOrElse(""))
  private val destLang = Property(ClientConf.langs.lift(1).getOrElse(""))
  private val langs = SeqProperty(ClientConf.langs)

  private val content = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(12),h2("Admin")),
    div(BootstrapCol.md(3),h3("Box Set-up"),
      ul(ClientConf.style.spacedList,
        li(
          a("Forms", Navigate.click(EntityTableState(EntityKind.BOX_FORM.kind,"form",None))),
        ),
        li(
          a("Pages", Navigate.click(EntityTableState(EntityKind.BOX_FORM.kind,"page",None))),
        ),
        li(
          a("Function builder", Navigate.click(EntityTableState(EntityKind.BOX_FORM.kind,"function",None)))
        ),
        li(
          div(
            p("Generate Form template for"),
            Select( entityForStub, viewModel.subSeq(_.entities))(Select.defaultLabel).render," ",
            button(ClientConf.style.boxButtonImportant,"Generate", onclick :+= presenter.generateStub(entityForStub.get))
          )
        )
      )
    ),
    div(BootstrapCol.md(3),h3("News"),
      ul(ClientConf.style.spacedList,
        li(
          a("News editor", Navigate.click(EntityTableState(EntityKind.BOX_FORM.kind,"news",None)))
        )
      )
    ),
    div(BootstrapCol.md(3),h3("Conf"),
      ul(ClientConf.style.spacedList,
        li(
          a("Conf", Navigate.click(AdminConfState))
        ),
        li(
          a("UI Conf", Navigate.click(AdminUiConfState))
        ),
        li(
          a("Import/Export Definitions", Navigate.click(AdminBoxDefinitionState))
        ),
        li(
          a("Labels", Navigate.click(FormPageState(EntityKind.BOX_FORM.kind,"labels","true",false)))
        ),
        div(
          h4("Translations"),
          "From: ",
          Select( sourceLang, langs)(Select.defaultLabel,width := 50.px).render,
          " to: ",
          Select( destLang, langs)(Select.defaultLabel,width := 50.px).render," ",
          button(ClientConf.style.boxButtonImportant,"Edit", Navigate.click(AdminTranslationsState(sourceLang.get,destLang.get)))
        )
      )
    )
  )


  override def getTemplate: Modifier = content

}
