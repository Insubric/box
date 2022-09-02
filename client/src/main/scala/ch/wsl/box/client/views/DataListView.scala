package ch.wsl.box.client.views


/**
  * Created by andre on 4/3/2017.
  */

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels, Navigate}
import ch.wsl.box.client.styles.{BootstrapCol}
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.form.UdashForm
import io.udash.core.Presenter
import org.scalajs.dom.{Element, Event}
import ch.wsl.box.client.{DataListState, DataState}
import ch.wsl.box.client.views.components.widget.WidgetUtils
import ch.wsl.box.model.shared.ExportDef
import scalatags.generic

case class DataList(list:Seq[ExportDef], currentEntity:Option[ExportDef], search:String, filteredList:Seq[ExportDef], kind:String)

object DataList extends HasModelPropertyCreator[DataList] {
  implicit val blank: Blank[DataList] =
    Blank.Simple(DataList(Seq(),None,"",Seq(),""))
}


case class DataListViewPresenter(modelName:String) extends ViewFactory[DataListState] {



  override def create(): (View, Presenter[DataListState]) = {
    val model = ModelProperty.blank[DataList]

    val presenter = new DataListPresenter(model)
    val view = new DataListView(model,presenter)
    (view,presenter)
  }
}

class DataListPresenter(model:ModelProperty[DataList]) extends Presenter[DataListState] {


  import ch.wsl.box.client.Context._

  override def handleState(state: DataListState ): Unit = {
    model.subProp(_.kind).set(state.kind)
//    println(state.currentExport)
    services.rest.dataList(state.kind,services.clientSession.lang()).map{ exports =>
      model.subSeq(_.list).set(exports)
      model.subSeq(_.filteredList).set(exports)
      val current = exports.find(_.function == state.currentExport)

      model.subProp(_.currentEntity).set(current)
    }
  }


  def updateExportsList() = {
    model.subProp(_.filteredList).set(model.subProp(_.list).get.filter(m => m.label.contains(model.get.search)))
  }

}

class DataListView(model:ModelProperty[DataList], presenter: DataListPresenter) extends ContainerView {
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._


  val sidebarGrid = BootstrapCol.md(2)
  def contentGrid =  BootstrapCol.md(10)

  override def renderChild(view: Option[View]): Unit = {

    import io.udash.wrappers.jquery._
    jQ(content).children().remove()
    if(view.isDefined) {
      view.get.getTemplate.applyTo(content)
    }

  }


  private val content: Element = div().render

  private def sidebar: Element = div(sidebarGrid)(
    Labels.exports.search,
    TextInput(model.subProp(_.search))(onkeyup :+= ((ev: Event) => presenter.updateExportsList())),
      produce(model.subProp(_.search)) { q =>
        ul(ClientConf.style.noBullet)(
          repeat(model.subSeq(_.filteredList)){m =>
            li(produce(m) { export =>
              WidgetUtils.addTooltip(m.get.tooltip) (a(Navigate.click(DataState(model.get.kind,export.function)), m.get.label).render)._1
            }).render
          }
        ).render
      }
    ).render

  override def getTemplate: scalatags.generic.Modifier[Element] = div(BootstrapStyles.Grid.row)(
    sidebar,
    div(contentGrid)(
      div(h1(Labels.exports.title)).render,
      produce(model)( m =>
        m.currentEntity match {
          case None => div(
            p(Labels.exports.select)
          ).render
          case Some(model) => div().render
        }
      ),
      content
    )
  )
}
