package ch.wsl.box.client.views.admin

import ch.wsl.box.client._
import ch.wsl.box.client.services.{ClientConf, Navigate, Notification}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.model.shared._
import ch.wsl.box.model.shared.admin.{ChildForm, FormCreationRequest}
import ch.wsl.typings.choicesJs.anon.PartialOptions
import ch.wsl.typings.choicesJs.mod
import ch.wsl.typings.choicesJs.publicTypesSrcScriptsInterfacesInputChoiceMod.InputChoice
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, document}
import scalacss.ScalatagsCss._

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

case class AdminCreateFormViewModel(entitites:Seq[String],request:FormCreationRequest, roles:Seq[String])
object AdminCreateFormViewModel extends HasModelPropertyCreator[AdminCreateFormViewModel] {
  implicit val blank: Blank[AdminCreateFormViewModel] =
    Blank.Simple(AdminCreateFormViewModel(Seq(),FormCreationRequest.base(""),Seq()))
}

object AdminFormCreateViewPresenter extends ViewFactory[AdminCreateFormState.type]{

  val prop = ModelProperty.blank[AdminCreateFormViewModel]

  override def create() = {
    val presenter = new AdminCreateFormPresenter(prop)
    (new AdminCreateFormView(prop,presenter),presenter)
  }
}

class AdminCreateFormPresenter(viewModel:ModelProperty[AdminCreateFormViewModel]) extends Presenter[AdminCreateFormState.type] {

  import Context._
  import ch.wsl.box.client.Context.Implicits._

  override def handleState(state: AdminCreateFormState.type): Unit = {
    for{
      entitites <- services.rest.entities(EntityKind.ENTITY.kind)
      roles <- services.rest.roles()
    } yield {
      val request = FormCreationRequest.base(entitites.head)
      viewModel.set(AdminCreateFormViewModel(entitites,request,roles))
    }
  }


}

class AdminCreateFormView(viewModel:ModelProperty[AdminCreateFormViewModel], presenter:AdminCreateFormPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._



  import Context._
  import Context.Implicits._

  def request = viewModel.subProp(_.request)

  def renderChild(entities:SeqProperty[String], entity:Property[String],childs:SeqProperty[ChildForm],removeButton:Modifier = Seq[Modifier](), nested: Binding.NestedInterceptor = Binding.NestedInterceptor.Identity):Modifier = {

    val childEntities:SeqProperty[String] = SeqProperty.blank
    entity.listen({ e =>
      if(e.nonEmpty) {
        services.rest.childCandidates(e).foreach { candidates =>
          childEntities.set(candidates)
        }
      }
    },true)

    def removeChildButton(i: ReadableProperty[Int]) =
      button(ClientConf.style.boxIconButton,Icons.x,onclick :+= ((e:Event) => childs.remove(i.get,1)))

    def addButton() =
      button(ClientConf.style.boxButton,"Add ",bind(entity)," child",onclick :+= {(e:Event) =>
        childs.append(ChildForm(childEntities.get.head,Seq()))
      })




    Seq(
      div(
        ClientConf.style.adminCreateForm,
        Select(entity,entities)(x => x),
        removeButton
      ),
      nested(showIf(childEntities.transform(_.nonEmpty)) {
        ul(
          nested(repeatWithIndex(childs) { (child,i,nested) =>
            li(
              renderChild(childEntities,child,removeChildButton(i),nested),

            ).render
          }),
          li(addButton())
        ).render
      })
    )
  }

  def renderChild(entities:SeqProperty[String],child: Property[ChildForm],removeButton:Modifier,nested: Binding.NestedInterceptor):Modifier = {

    val entity = child.bitransform(x => x.entity)(e => child.get.copy(entity = e, childs = Seq()))
    val childs = child.bitransformToSeq(x => x.childs)(e => child.get.copy(childs = e.toSeq))




    renderChild(entities,entity, childs, removeButton,nested)

  }



  def rolesRenderer = {
    val el = select(multiple).render


    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(el)) {
        observer.disconnect()
        val options = PartialOptions()

        options
          .setRemoveItemButton(true)
          .setDuplicateItemsAllowed(false)
        val choicesJs = new mod.default(el,options)

        dom.window.asInstanceOf[js.Dynamic].choices = choicesJs

        el.addEventListener("change",(e:Event) => {
          (choicesJs.getValue(true):Any) match {
            case list: js.Array[String] => request.set(request.get.copy(roles = list.toSeq))
            case a: String => request.set(request.get.copy(roles = Seq(a)))
          }
        })

        viewModel.subProp(_.roles).listen(values => {
          val choices: Seq[InputChoice] = values.map { cs =>
            InputChoice(cs,cs)
          }
          choicesJs.asInstanceOf[js.Dynamic].setChoices(choices.toJSArray,"value","label",true)
        },true)
      }
    })

    observer.observe(document,MutationObserverInit(childList = true, subtree = true))
    el
  }


  private val content = {

    val entity = request.bitransform(x => x.main_entity)(e => request.get.copy(main_entity = e, childs = Seq()))
    val childs =  request.bitransformToSeq(x => x.childs)(e =>  request.get.copy(childs = e.toSeq))

    div(margin := 50.px,
        h2("Create form"),
        div(
          h5("Name"),
          TextInput(request.bitransform(_.name)(x => request.get.copy(name = x)))(marginBottom := 20.px),
          h5("Select main table"),
          renderChild(viewModel.subSeq(_.entitites),entity, childs),
          h5(Checkbox(request.bitransform(_.add_to_home)(x => request.get.copy(add_to_home = x)))()," Add to homepage",marginTop := 10.px),
          h5("Roles"),
          rolesRenderer,
          button(ClientConf.style.boxButtonImportant,"Create", onclick :+= {(e:Event) => services.rest.createForm(request.get).foreach { form_uuid =>
            Navigate.to(EntityTableState(EntityKind.FORM.kind,request.get.name,None,false))
          }}),
        ),

    )
  }


  override def getTemplate: Modifier = content

}

