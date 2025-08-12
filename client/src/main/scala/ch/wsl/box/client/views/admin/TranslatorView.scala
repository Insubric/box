package ch.wsl.box.client.views.admin

import ch.wsl.box.client._
import ch.wsl.box.client.services.{ClientConf, Navigate}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.model.shared._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.Event
import scalacss.ScalatagsCss._


object TranslatorViewPresenter extends ViewFactory[TranslatorState.type]{


  override def create() = {
    val presenter = new TranslatorPresenter()
    (new TranslatorView(presenter),presenter)
  }
}

class TranslatorPresenter() extends Presenter[TranslatorState.type] {

  import Context._
  import ch.wsl.box.client.Context.Implicits._

  override def handleState(state: TranslatorState.type): Unit = {

  }

}

class TranslatorView(presenter:TranslatorPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._

  private val sourceLang = Property(ClientConf.langs.headOption.getOrElse(""))
  private val destLang = Property(ClientConf.langs.lift(1).getOrElse(""))
  private val langs = SeqProperty(ClientConf.langs)

  private val content = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(12),h2("Translations")),

    div(BootstrapCol.md(3),h3("Conf"),

          "From: ",
          Select( sourceLang, langs)(Select.defaultLabel,width := 50.px).render,
          " to: ",
          Select( destLang, langs)(Select.defaultLabel,width := 50.px).render," ",
          button(ClientConf.style.boxButtonImportant,"Edit", Navigate.click(AdminTranslationsState(sourceLang.get,destLang.get)))


    )
  )


  override def getTemplate: Modifier = content

}
