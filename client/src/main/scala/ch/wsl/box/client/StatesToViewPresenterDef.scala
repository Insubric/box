package ch.wsl.box.client

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.UI
import io.udash._
import ch.wsl.box.client.views._
import ch.wsl.box.client.views.admin.{AdminFormCreateViewPresenter, AdminViewPresenter, BoxDefinitionViewPresenter, ConfViewPresenter, DBReplViewPresenter, TranslationsViewPresenter, TranslatorViewPresenter, UiConfViewPresenter}
import ch.wsl.box.model.shared.EntityKind

class StatesToViewPresenterDef extends ViewFactoryRegistry[RoutingState] {
  def matchStateToResolver(state: RoutingState): ViewFactory[_ <: RoutingState] = state match {
    case RootState(layout) => RootViewPresenter
    case IndexState => {
      UI.indexPage match {
        case Some(value) => EntityFormViewPresenter
        case None => IndexViewPresenter
      }
    }
    case l:LoginStateAbstract => LoginViewPresenter
    case AuthenticateState(provider_id) => AuthenticateView
    case EntitiesState(kind,currentEntity,public,layout) => EntitiesViewPresenter(kind,currentEntity)
    case EntityTableState(kind,entity,query,public) => EntityTableViewPresenter(Routes(kind,entity,public))
    case EntityFormState(kind,entity,write,id,public,layout) => EntityFormViewPresenter
    case FormPageState(kind,entity,write,public,layout) => EntityFormViewPresenter
    //case MasterChildState(_,master,child) => MasterChildViewPresenter(master,child)
    case DataState(_,_) => DataViewPresenter
    case DataListState(_,_) => DataListViewPresenter
    case TranslatorState => TranslatorViewPresenter
    case AdminState => AdminViewPresenter
    case AdminBoxDefinitionState => BoxDefinitionViewPresenter
    case AdminTranslationsState(_,_) => TranslationsViewPresenter
    case AdminConfState => ConfViewPresenter
    case AdminUiConfState => UiConfViewPresenter
    case AdminDBReplState => DBReplViewPresenter
    case AdminCreateFormState => AdminFormCreateViewPresenter
    case _ => ErrorViewPresenter
  }
}
