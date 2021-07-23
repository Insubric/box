package ch.wsl.box.client.views

import ch.wsl.box.client.services.{ClientConf, LoginPopup, Notification, REST, UI}
import ch.wsl.box.client.styles.GlobalStyles
import io.udash._
import ch.wsl.box.client._
import org.scalajs.dom.Element
import scalatags.JsDom.tags2.main
import ch.wsl.box.client.views.components._
import io.udash.bootstrap.BootstrapStyles
import io.udash.core.Presenter
import scalacss.ScalatagsCss._


case class RootViewModel(layout:String)
object RootViewModel extends HasModelPropertyCreator[RootViewModel] {
  implicit val blank: Blank[RootViewModel] =
    Blank.Simple(RootViewModel(Layouts.std))
}

case object RootViewPresenter extends ViewFactory[RootState]{


  override def create(): (View, Presenter[RootState]) = {

    val prop = ModelProperty.blank[RootViewModel]

    (new RootView(prop),new RootPresenter(prop))
  }
}

class RootPresenter(viewModel:ModelProperty[RootViewModel]) extends Presenter[RootState] {



  override def handleState(state: RootState): Unit = {
    viewModel.set(RootViewModel(state.layout))
  }
}

class RootView(viewModel:ModelProperty[RootViewModel]) extends ContainerView {
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  private val child: Element = div().render


  private val notifications = div(ClientConf.style.notificationArea,
    produce(Notification.list){ notices =>
      notices.map { notice =>
        div(ClientConf.style.notification, notice).render
      }
    }
  )


  private def content = produce(viewModel.subProp(_.layout)) {
      case Layouts.std => {
        div(BootstrapStyles.containerFluid)(
          Header.navbar(UI.title),
          notifications,
          main(ClientConf.style.fullHeight)(
            div()(
              child
            )
          ),
          Footer.template(UI.logo),
          LoginPopup.render
        ).render
      }
      case Layouts.blank => div(BootstrapStyles.containerFluid,overflowX.hidden)(
        notifications,
        child
      ).render
    }



  override def getTemplate: Modifier = content

  override def renderChild(view: Option[View]): Unit = {
    import io.udash.wrappers.jquery._
    jQ(child).children().remove()
    view.foreach(_.getTemplate.applyTo(child))
  }
}
