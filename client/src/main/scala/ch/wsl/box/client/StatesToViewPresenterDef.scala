package ch.wsl.box.client

import ch.wsl.box.client.routes.Routes
import io.udash._
import ch.wsl.box.client.views._
import ch.wsl.box.client.views.admin.{AdminViewPresenter, BoxDefinitionViewPresenter, ConfViewPresenter, UiConfViewPresenter}

class StatesToViewPresenterDef extends ViewFactoryRegistry[RoutingState] {
  def matchStateToResolver(state: RoutingState): ViewFactory[_ <: RoutingState] = state match {
    case RootState => RootViewPresenter
    case IndexState => IndexViewPresenter
    case l:LoginStateAbstract => LoginViewPresenter
    case EntitiesState(kind,currentEntity) => EntitiesViewPresenter(kind,currentEntity,2)
    case EntityTableState(kind,entity,query) => EntityTableViewPresenter(Routes(kind,entity))
    case EntityFormState(kind,entity,write,id,public) => EntityFormViewPresenter
    case FormPageState(kind,entity,write,public) => EntityFormViewPresenter
    //case MasterChildState(_,master,child) => MasterChildViewPresenter(master,child)
    case DataState(_,_) => DataViewPresenter
    case DataListState(_,currentExport) => DataListViewPresenter(currentExport)
    case AdminState => AdminViewPresenter
    case AdminBoxDefinitionState => BoxDefinitionViewPresenter
    case AdminConfState => ConfViewPresenter
    case AdminUiConfState => UiConfViewPresenter
    case _ => ErrorViewPresenter
  }
}
