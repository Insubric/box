package ch.wsl.box.client.views

import io.udash._
import ch.wsl.box.client._
import ch.wsl.box.client.services.{ClientConf, REST, ServiceModule, UI}
import org.scalajs.dom.{Element, Event}
import ch.wsl.box.client.styles.{BootstrapCol}
import ch.wsl.box.model.shared.{EntityKind, JSONQuery, JSONSort, NewsEntry, Sort}
import io.circe.Json
import scalacss.ScalatagsCss._
import ch.wsl.box.client.views.components.Debug
import io.udash.bootstrap.BootstrapStyles

import scala.concurrent.Future

case class IndexViewModel(news:Seq[NewsEntry])
object IndexViewModel extends HasModelPropertyCreator[IndexViewModel] {
  implicit val blank: Blank[IndexViewModel] =
    Blank.Simple(IndexViewModel(Seq()))
}

object IndexViewPresenter extends ViewFactory[IndexState.type ]{

  val prop = ModelProperty.blank[IndexViewModel]

  override def create() = (new IndexView(prop),new IndexPresenter(prop))
}

class IndexPresenter(viewModel:ModelProperty[IndexViewModel]) extends Presenter[IndexState.type ] {

  import Context._


  override def handleState(state: IndexState.type ): Unit = {
    for{
      news <- if(ClientConf.displayIndexNews)
          services.rest.news(services.clientSession.lang())
        else
          Future.successful(Seq())
    } yield {
      viewModel.set(IndexViewModel(news))
    }
  }
}

class IndexView(viewModel:ModelProperty[IndexViewModel]) extends View {
  import scalatags.JsDom.all._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.JSONUtils._
  import io.udash.css.CssView._


  import org.scalajs.dom.File


  private val content = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(12),raw(UI.indexTitle)),
    if(ClientConf.displayIndexHtml){
      div(BootstrapCol.md(12),raw(UI.indexHtml))
    } else div(),
    if(ClientConf.displayIndexNews) {
      div(BootstrapCol.md(12),h2("News"),
        Debug(viewModel, name = "indexView"),
        repeat(viewModel.subSeq(_.news)) { news =>
          div(
            div(news.get.datetime),
            news.get.title.map(h4(_)),
            div(raw(news.get.text)),
            br
          ).render
        }
      )
    } else div()
  )


  override def getTemplate: Modifier = content

}
