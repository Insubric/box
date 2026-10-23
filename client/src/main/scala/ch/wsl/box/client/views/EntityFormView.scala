package ch.wsl.box.client.views

import ch.wsl.box.client.db.{DB, LocalRecord}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.{Context, EntityFormState, EntityTableState, FormState}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels, Navigate, Navigation, Navigator, Notification, Record}
import ch.wsl.box.client.styles.{BootstrapCol, Fade, Icons}
import ch.wsl.box.client.utils.HTMLFormElementExtension.HTMLFormElementExt
import ch.wsl.box.client.utils._
import ch.wsl.box.client.views.components.ui.Stepper
import ch.wsl.box.client.views.components.widget.{Widget, WidgetCallbackActions}
import ch.wsl.box.client.views.components.{Debug, JSONMetadataRenderer, ModalStack}
import ch.wsl.box.client.views.elements.Offline
import ch.wsl.box.client.views.helpers.{ActionRenderer, ActionRigths, FormActionHandler, PopupFrame, VoiceHelper}
import ch.wsl.box.model.shared._
import ch.wsl.box.model.shared.errors.SQLExceptionReport
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.{Json, JsonNumber, JsonObject}
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.badge.UdashBadge
import io.udash.bootstrap.utils.BootstrapStyles.Color
import io.udash.bootstrap.utils.UdashIcons
import io.udash.{showIf, _}
import io.udash.bootstrap.{BootstrapStyles, UdashBootstrap}
import io.udash.component.ComponentId
import io.udash.core.Presenter
import io.udash.css.CssStyleName
import io.udash.properties.single.Property
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, HTMLFormElement}
import scribe.Logging

import scala.concurrent.{Future, Promise}
import scalatags.JsDom
import scalacss.ScalatagsCss._
import scalacss.internal.StyleA
import ch.wsl.typings.hotkeysJs.mod.{HotkeysEvent, KeyHandler}
import io.udash.utils.Registration
import org.scalajs.dom

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.scalajs.js.URIUtils
import scala.language.reflectiveCalls
import scala.scalajs.js
import scala.scalajs.js.timers.setTimeout
import scala.util.{Failure, Success, Try}

/**
  * Created by andre on 4/24/2017.
  */

case class EntityFormModel(name:String, kind:String, id:Option[String], metadata:Option[JSONMetadata], originalData:Json,data:Json,
                           error:String, children:Seq[JSONMetadata], navigation: Navigation, changed:Boolean, write:Boolean, public:Boolean, insert:Boolean, showActionPanelMobile: Boolean, localData:Boolean, updateRight:Boolean, popup:Boolean)

object EntityFormModel extends HasModelPropertyCreator[EntityFormModel] {

  val empty = EntityFormModel("","",None,None,Json.Null,Json.Null,"",Seq(), Navigation.empty0,false, true, false, true, false,false,false, popup = false)

  implicit val blank: Blank[EntityFormModel] = Blank.Simple(empty)
}

class EntityFormViewPresenter extends ViewFactory[FormState] {
  val model = ModelProperty.blank[EntityFormModel]
  val presenter = EntityFormPresenter(model)

  override def create(): (View, Presenter[FormState]) = {
    (EntityFormView(model,presenter),presenter)
  }
}

object EntityFormCostants {
  val HIDE_HEADER: String = "hideHeader"
  val HIDE_FOOTER: String = "hideFooter"
  val HIDE_ID: String     = "hideID"

  val MAX_WIDTH: String = "maxWidth"
  val STEPPER: String   = "stepper"
}

case class EntityFormPresenter(model:ModelProperty[EntityFormModel]) extends Presenter[FormState] with Logging {
  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._


  import EntityFormCostants._

  TestHooks.setData(model.subProp(_.data))

  var currentState:FormState = null
  override def handleState(state: FormState): Unit = {

    currentState = state

    logger.warn(state.toString)

    val loaded = Promise[Boolean]()
    TestHooks.addLoadedPromise(loaded)

    services.clientSession.loading.set(true)

    model.subProp(_.kind).set(state.kind)
    model.subProp(_.name).set(state.entity)
    model.subProp(_.id).set(state.id)
    model.subProp(_.insert).set(state.id.isEmpty)


    {for{
      metadata <- services.rest.metadata(state.kind, services.clientSession.lang(), state.entity,state.public).map{ m =>
        if(state.popup) {
          val p = m.params.getOrElse(Json.obj()).deepMerge(Json.obj(
            HIDE_FOOTER -> Json.True,
            HIDE_HEADER -> Json.True
          ))
          m.copy(params = Some(p))
        } else {
          m
        }
      }
      rowAccess <- state.id.flatMap(x => JSONID.fromString(x,metadata)) match {
        case Some(id) => services.rest.rowAccess(metadata.entity,state.kind,id.query)
        case None => Future.successful(true)
      }
      children <- if(Seq(EntityKind.FORM,EntityKind.BOX_FORM).map(_.kind).contains(state.kind)) services.rest.children(state.kind,state.entity,services.clientSession.lang(),state.public) else Future.successful(Seq())
      record <- state.id match {
        case Some(id) => {
          val jsonId = state.id.flatMap(x => JSONID.fromString(x,metadata)) match {
            case Some(value) => value
            case None => throw new Exception(s"cannot parse JsonID ${state.id}")
          }
          services.data.get(state.kind, services.clientSession.lang(), state.entity,jsonId,state.public)
        }
        case None => Future.successful(Record(Json.Null,false))
      }
    } yield {

      BrowserConsole.log(record.data)

      var insert = false

      //check if data is already present for the given id
      val dataWithId = if(record.data == Json.Null) { // if not we are going to do an insert
        insert = true
        val d = Json.obj(JSONMetadata.jsonPlaceholder(metadata,children).toSeq :_*) // taking the defaults
        state.id.flatMap(JSONID.fromString(_, metadata)) match {
          case Some(id) => d.deepMerge(Json.fromFields(id.toFields))
          case None => d
        }
      } else {
        record.data
      }

      val dataWithQueryParams = state.data match {
        case Some(value) => dataWithId.deepMerge(JSONUtils.toJs(value,metadata))
        case None => dataWithId
      }


      BrowserConsole.log(dataWithQueryParams)

      val stateModel = EntityFormModel(
        name = state.entity,
        kind = state.kind,
        id = state.id,
        metadata = Some(metadata),
        originalData = dataWithQueryParams,
        data = dataWithQueryParams,
        "",
        children,
        Navigation.empty1,
        false,
        state.writeable && rowAccess,
        state.public,
        insert,
        false,
        popup = state.popup,
        localData = record.local_version,
        updateRight = rowAccess
      )

      model.set(stateModel)

      resetChanges()


      setNavigation()

      widget.afterRender().map{ _ =>
        enableGoAway("handleState")
        services.clientSession.loading.set(false)
        if(!loaded.isCompleted)
          loaded.success(true)
      }



    }}.onComplete {
      case Failure(exception) => {
        exception.printStackTrace()
        throw exception
      }
      case Success(value) => true
    }

  }

  import io.circe.syntax._

//  val saveKey:KeyHandler = (event:KeyboardEvent,handler:HotkeysEvent) => {
//    event.preventDefault()
//    save().map{ case (id,d) => afterSave(id,d)}
//    false
//  }
//
//  val hotkeysOptions = ch.wsl.typings.hotkeysJs.mod.Options().setCapture(true).setKeydown(true).setElement(document)

  def focusField = document.activeElement.closest("*[data-box-class='widget']").asInstanceOf[dom.HTMLElement].dataset("boxField")

  val keyListeners = (event:KeyboardEvent) => {
    // Check for Ctrl+S
    if (event.ctrlKey && event.key == "s") {
      event.preventDefault()
      actionHandler.save()
    }

    if (event.ctrlKey && event.key == "c") {
      if(
        window.getSelection().isCollapsed && //any part of the page is selected
          Try(document.activeElement.asInstanceOf[js.Dynamic].selectionStart == document.activeElement.asInstanceOf[js.Dynamic].selectionEnd).getOrElse(true)
      ) {
        event.preventDefault()
        model.subProp(_.data).get.jsOpt(focusField).foreach { js =>
          window.navigator.clipboard.writeText(js.noSpaces)
        }
      }
    }

    if (event.ctrlKey && event.key == "v") {
      document.activeElement match {
        case element: HTMLInputElement => ()
        case element: HTMLTextAreaElement => ()
        case element:dom.HTMLElement if element.contentEditable == "true" => ()
        case element:dom.HTMLElement => {
          event.preventDefault()
          println("PasingAAAA")
          window.navigator.clipboard.readText().toFuture.foreach{ str =>
            println(str)
            val focus = focusField
            println(focus)
            val metadata = model.subProp(_.metadata).get

            for{
              m <- metadata
              f <- m.fields.find(_.name == focus)
              js <- JSONUtils.toJs(str,f)
            } yield {
              BrowserConsole.log(js)
              val d = model.subProp(_.data)
              d.set(d.get.deepMerge(Json.fromFields(Map(focus -> js))))
            }

          }
        }
      }
    }


  }
  document.addEventListener("keydown",keyListeners)
//  ch.wsl.typings.hotkeysJs.mod.default("ctrl+s",hotkeysOptions,saveKey)

  var mainBinding:Option[Binding] = None

  override def onClose(): Unit = {
    if(Navigate.canGoAway) {
      super.onClose()
      logger.debug("onClose")
      changesListener.cancel()
      Try {
        if (widget != null) {
          widget.killWidget()
        }
      }
      model.set(EntityFormModel.empty, true)
      enableGoAway("onClose")
      ch.wsl.typings.hotkeysJs.mod.default.unbind()
      document.removeEventListener("keydown",keyListeners)
      mainBinding.foreach(_.kill())
      listenerManager.clearAll()
    }

  }

  private var _form:HTMLFormElement = scalatags.JsDom.all.form().render

  def setForm(form:HTMLFormElement)= { _form = form }



  def beforeSave(check:Boolean = true):Future[Json]  = {

    services.clientSession.loading.set(true)

    if(check) if(!_form.reportValidity()) {
      services.clientSession.loading.set(false)
      val errors = document.querySelectorAll("*:invalid")
      for(i <- 0 to errors.length) {
        errors.item(i) match {
          case e:dom.HTMLElement => {
            logger.warn(s"Error on: ${e} cass: ${e.className}")
            window.asInstanceOf[js.Dynamic].testEL = e
            e.closest(".box-tab") match {
              case element: dom.HTMLElement => element.dataset.get("boxTab") match {
                case Some(value) => document.getElementById(JSONMetadataRenderer.tabSelectorId(value)) match {
                  case element: dom.HTMLElement => {
                    element.click()
                    _form.reportValidity()
                  }
                  case _ => ???
                }
                case None => logger.warn("Box tab element not found")
              }
              case _ => logger.warn("Non HTMLElement box-tab")
            }
          }
          case e => logger.warn(s"Error on non HTMLElement: ${e}")
        }
      }
      logger.warn(s"Form validation failed")

      return Future.failed(new Exception("Validation failed"))
    }

    val m = model.get
    val metadata = m.metadata.get
    val data:Json = m.data


    {for{
      updatedData <- widget.beforeSave(data,metadata)
      updatedDataNoNoData= updatedData.removeNonDataFields(metadata,m.children)

    } yield {
      logger.debug(s"""outcome

                 Save outcome:

                 original: $data

                 afterBeforeSave: $updatedDataNoNoData

                 """)



      updatedDataNoNoData


    }}.recover{ case e =>
      e.getStackTrace.foreach(x => logger.error(s"file ${x.getFileName}.${x.getMethodName}:${x.getLineNumber}"))
      e.printStackTrace()
      services.clientSession.loading.set(false)
      throw e
    }

  }

  def afterSave(id:JSONID,data:Record) = {
    val currentState = model.get
    val newData = currentState.data.deepMerge(data.data)

    if(!model.get.id.contains(id.asString)) {
        val state = currentState.asInstanceOf[EntityFormState]
        Navigate.to(state.copy(_id = Some(id.asString)))
    } else {
      model.set(currentState.copy(
        data = newData,
        originalData = newData,
        id = Some(id.asString),
        insert = false,
        changed = false,
        localData =  data.local_version
      ))
      childChanged.set(false)
      resetChanges()
    }
    enableGoAway("afterSave")

    services.clientSession.loading.set(false)
  }

  def reload(id:JSONID): Future[Json] = {
    services.clientSession.loading.set(true)
    for{
      resultSaved <- services.data.get(model.get.kind, services.clientSession.lang(), model.get.name, id,model.get.public)
      result <- if(!model.get.id.contains(id.asString)) {
        Future.successful{
          val state = currentState.asInstanceOf[EntityFormState]
          Navigate.to(state.copy(_id = Some(id.asString)))
          Json.Null
        }
      } else {



        val promise = Promise[Json]()
        reset()

        model.set(model.get.copy(
          data = resultSaved.data,
          originalData = resultSaved.data,
          id = Some(id.asString),
          localData = resultSaved.local_version
        ))

        resetChanges()


        enableGoAway("reload")
        widget.afterRender().foreach{ _ =>
          services.clientSession.loading.set(false)
          promise.success(model.subProp(_.data).get)
        }
        promise.future
      }
    } yield result
  }

  def reload():Future[Json] = {
    model.subProp(_.id).get.flatMap(x => JSONID.fromString(x,model.get.metadata.get)) match {
      case Some(id) => reload(id)
      case None => {
        logger.warn("Cannot revert with no ID")
        Future.successful(Json.Null)
      }
    }
  }

  def delete(action:FormAction) = {

      for{
        name <- model.get.metadata.map(_.name)
        key <- model.get.id.flatMap(x => JSONID.fromString(x,model.get.metadata.get))
      } yield {
        services.data.delete(model.get.kind, services.clientSession.lang(),name,key).map{ count =>
          Notification.add("Deleted " + count.count + " rows")
          Routes.getUrl(action,model.get.data,model.get.kind,model.get.name,Some(key.asString),model.get.write) match {
            case Some(value) => Navigate.toUrl(value)
            case None => Navigate.to(Routes(model.get.kind, name,model.subProp(_.public).get).entity(name))
          }

        }
      }

  }

  def reset(): Unit = {
    model.subProp(_.data).set(Json.Null)
    model.subProp(_.originalData).set(Json.Null)
    model.subProp(_.id).set(None)
    enableGoAway("reset")
  }

  def duplicate():Unit = {
    if(model.subProp(_.changed).get && !window.confirm(Labels.navigation.goAway)) {
      return
    }
    val oldModel = model.get
    model.set(oldModel.copy(
      id = None,
      data = oldModel.metadata.map{ metadata =>
        metadata.keys.foldLeft(oldModel.data)((data,key) => data.hcursor.downField(key).delete.top.get) //removes key from json
      }.getOrElse(oldModel.data)
    ))

  }


  def setNavigation() = {
    services.navigator.For(model.get.id,model.get.metadata.get,model.subProp(_.public).get).navigation().map{ nav =>
      logger.info(s"Navigation $nav")
      model.subProp(_.navigation).set(nav)
    }
  }

  private var widget:Widget = new Widget {
    import scalatags.JsDom.all._

    override def field: JSONField = JSONField.empty

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div()
    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div()
  }

  private val childChanged:Property[Boolean] = Property(false)

  def autoSave = model.get.metadata.flatMap(_.params).exists(_.js("autoSave") == Json.True)

  val debounceSave = Debounce(2000.millis) { (_:Unit) =>
    actionHandler.save()
  }

  val changesListener = childChanged.listen { hasChanges =>
    logger.debug(s"Childchanged listener to $hasChanges")
    if(hasChanges) {
      model.subProp(_.changed).set(true)
      avoidGoAway
      if(autoSave)
        debounceSave()

    } else {
      enableGoAway("changeListener")
    }
  }

  def loadWidgets(f:JSONMetadata) = {
    val actions = WidgetCallbackActions(actionHandler.save,reload,() => {
      logger.debug(s"SetChanged callback")
      childChanged.set(true,true)
    })
    widget = JSONMetadataRenderer(f, model.subProp(_.data), model.subProp(_.children).get, model.subProp(_.id),actions,childChanged,model.subProp(_.public).get,model.subProp(_.popup).get)
    widget
  }

  def resetChanges() = widget match {
    case jmr:JSONMetadataRenderer => jmr.resetChanges()
    case _ => {}
  }



  def navigate(n: navigator.For => Future[Option[String]]) = {
    n(navigator.For(model.get.id, model.get.metadata.get,model.subProp(_.public).get)).map(_.map(goTo))
  }

  def next() = navigate(_.next())
  def prev() = navigate(_.previous())
  def first() = navigate(_.first())
  def last() = navigate(_.last())
  def nextPage() = navigate(_.nextPage())
  def prevPage() = navigate(_.prevPage())
  def firstPage() = navigate(_.firstPage())
  def lastPage() = navigate(_.lastPage())

  val navigator = services.navigator


  def goTo(id:String) = {
    val m = model.get
    val r = Routes(m.kind,m.name,m.public)
    val newState = if(model.get.write) {
      r.edit(id)
    } else {
      r.show(id)
    }
    Navigate.to(newState)
  }

  def avoidGoAway = {
    Navigate.disable{ () =>
      window.confirm(Labels.navigation.goAway)
    }
    model.subProp(_.changed).set(true)
    if(!currentState.popup) {
      window.onbeforeunload = { (e: BeforeUnloadEvent) =>
        if (Context.applicationInstance.currentState.isInstanceOf[EntityFormState] || Context.applicationInstance.currentState.isInstanceOf[EntityTableState]) {
          e.preventDefault()
          Labels.navigation.goAway
        }
      }
    }
  }
  def enableGoAway(where:String) = {
    logger.debug(s"enableGoAway from $where")
    Navigate.enable()
    model.subProp(_.changed).set(false)
    window.onbeforeunload = { (e:BeforeUnloadEvent) => }
  }

  val showNavigation:ReadableProperty[Boolean] = model.subProp(_.public).transform(x => !x)
    .combine {
      model.subProp(_.metadata).transform(x => !x.exists(_.static))
    }(_ && _)
    .combine {
      model.subProp(_.metadata).transform(x => x.exists(_.action.showNavigation))
    }(_ && _)


  def actionHandler = new FormActionHandler(
    model.get.metadata.get,
    model.subProp(_.data),
    model.get.originalData,
    model.get.public,
    model.get.insert,
    () => model.subProp(_.showActionPanelMobile).set(false),
    {
      case Some(id) => reload(id)
      case None => reload()
    },
    this.beforeSave,
    this.afterSave,
    Unit => this.duplicate()
  )

  val listenerManager = new ListenerManager()

  def rights = ActionRigths(model.get.insert,model.get.updateRight,model.get.write)

  def roles() = services.clientSession.getRoles()


  def voiceHelper = new VoiceHelper(model.subProp(_.metadata).get.get)
}

case class EntityFormView(model:ModelProperty[EntityFormModel], presenter:EntityFormPresenter) extends View {
  import scalatags.JsDom.all._
  import io.circe.generic.auto._
  import io.udash.css.CssView._
  import Context.Implicits._

  import EntityFormCostants._


  def labelTitle = produceWithNested(model.subProp(_.metadata)) { (m,nested) =>


    val name = m.map(_.label).getOrElse(model.get.name)

    val nameProp:ReadableProperty[String] = m.flatMap(_.dynamicLabel) match {
      case None => Property(name)
      case Some(dl) => {
        model.subProp(_.data).transform(_.getOpt(dl).getOrElse(name))
      }
    }

    span(nested(bind(nameProp))).render
  }


  case class StepperOption(steps:Seq[String],current:String)


  def stepper(s:StepperOption) = {
    div(margin := "20px",
      Stepper.render(s.steps.map(st => Stepper.Step(st,"","")),s.steps.indexOf(s.current))
    )
  }

  override def getTemplate: scalatags.generic.Modifier[Element] = {

    def recordNavigation(nested:Binding.NestedInterceptor) = showIf(presenter.showNavigation){

          def navigation = model.subModel(_.navigation)

//            div(ClientConf.style.boxNavigationLabel,
//              Navigation.button(navigation.subProp(_.hasPreviousPage), presenter.firstPage, Labels.navigation.firstPage, _.Float.left()),
//              Navigation.button(navigation.subProp(_.hasPreviousPage), presenter.prevPage, Labels.navigation.previousPage, _.Float.left()),
//              span(
//                ClientConf.style.boxNavigationLabel,
//                " " + Labels.navigation.page + " ",
//                bind(model.subProp(_.navigation.currentPage)),
//                " " + Labels.navigation.of + " ",
//                bind(model.subProp(_.navigation.pages)),
//                " "
//              ),
//              Navigation.button(navigation.subProp(_.hasNextPage), presenter.lastPage, Labels.navigation.lastPage, _.Float.right()),
//              Navigation.button(navigation.subProp(_.hasNextPage), presenter.nextPage, Labels.navigation.nextPage, _.Float.right())
//            ),
//            div(BootstrapStyles.Visibility.clearfix),
            div(ClientConf.style.navigationBlock,
              Navigation.button(navigation.subProp(_.hasPrevious), presenter.first, i(UdashIcons.FontAwesome.Solid.fastBackward)),
              //Navigation.button(navigation.subProp(_.hasPrevious), presenter.prevPage, i(UdashIcons.FontAwesome.Solid.backward)),
              Navigation.button(navigation.subProp(_.hasPrevious), presenter.prev, i(UdashIcons.FontAwesome.Solid.caretLeft)),
              span(
                " " + Labels.navigation.record + " ",
                bind(model.subModel(_.navigation).subProp(_.currentIndex)),
                " " + Labels.navigation.of + " ",
                bind(model.subModel(_.navigation).subProp(_.count)),
                " "
              ),
              Navigation.button(navigation.subProp(_.hasNext), presenter.next, i(UdashIcons.FontAwesome.Solid.caretRight)),
              //Navigation.button(navigation.subProp(_.hasNext), presenter.nextPage, i(UdashIcons.FontAwesome.Solid.forward)),
              Navigation.button(navigation.subProp(_.hasNext), presenter.last, i(UdashIcons.FontAwesome.Solid.fastForward)),
            ).render
    }

    def actions(metadata:JSONMetadata,nested:Binding.NestedInterceptor,selector:FormActionsMetadata => Seq[FormAction]) = {

      val actionRenderer = new ActionRenderer(presenter.actionHandler,presenter.listenerManager,metadata,presenter.rights,Unit => presenter.reset())


      div(
        div(
          nested(produceWithNested(model.subProp(_.metadata)) { (form,realeser2) =>
            div(
              realeser2(produceWithNested(model.subProp(_.id)) { case (_id,releaser3) =>
                div(ClientConf.style.spaceBetween,
                  releaser3(produce(model.subProp(_.insert)) { _ =>
                    div(
                      form.toSeq.flatMap(f => selector(f.action)).map(actionRenderer.render(_id))
                    ).render
                  })
                ).render
              })
            ).render
          })
        ),
        div(BootstrapStyles.Visibility.clearfix)
      )
    }

    def formHeader(nested:Binding.NestedInterceptor,showId:Boolean,metadata:JSONMetadata) = div(ClientConf.style.formHeader,
      div( ClientConf.style.spaceBetween, marginBottom := 10.px,
        h3(
          ClientConf.style.noMargin,
          ClientConf.style.formTitle,
          labelTitle
        ),
        div(
          nested(showIf(model.subProp(_.changed)) {
            small(id := TestHooks.dataChanged,style := "color: red",Labels.form.changed).render
          })
        ),
        div(
          ClientConf.style.noMargin,
          ClientConf.style.formTitle,
          if(showId) {
            nested(showIf(model.subProp(_.metadata).transform(!_.exists(_.static))) {
              div(
                Offline(model.subProp(_.localData)),
                nested(produce(model.subProp(_.id)) { id =>
                  id.flatMap(JSONID.fromString(_,metadata)).toSeq.flatMap{ jsonId =>
                    jsonId.id.flatMap{ k =>
                      metadata.fields.find(_.name == k.key).flatMap(_.label).map { label =>
                        span(span(ClientConf.style.formTitleLight, label), " ", k.value.string, " ").render
                      }
                    }
                  }
                })
              ).render
            })
          } else frag(),
        )
      ),
//      div(ClientConf.style.mobileOnly,
//        button(ClientConf.style.boxButton,i(UdashIcons.FontAwesome.Solid.ellipsisV))
//      ),
      if(ClientConf.enableVoice) button(Icons.mic, ClientConf.style.bottomLeftButton, onclick :+= ((e:Event) => presenter.voiceHelper.selectField().map{x =>
        model.subProp(_.data).set(model.subProp(_.data).get.deepMerge(Json.obj(x.field.name -> x.value)))
      })) else Seq[Modifier](),
      div(ClientConf.style.spaceBetween,ClientConf.style.noMobile,
        actions(metadata,nested,_.actions(model.subProp(_.write).get,presenter.roles())),
        div(ClientConf.style.spaceAfter)(
          nested(showIf(presenter.showNavigation) {
            div(actions(metadata,nested,_.navigation(model.subProp(_.write).get,presenter.roles()))).render
          }),
          nested(showIf(model.transform(_.navigation.count > 1)) {
              div(recordNavigation(nested)).render
          })
        ).render,
      ),
      div(ClientConf.style.mobileOnly,
        nested(showIf(model.transform(_.navigation.count > 1)) {
          div(ClientConf.style.spaceBetween,recordNavigation(nested)).render
        }),
        button(ClientConf.style.mobileBoxAction)(i(UdashIcons.FontAwesome.Solid.pen), onclick :+= ((e:Event) => model.subProp(_.showActionPanelMobile).set(true))).render,
        div(ClientConf.style.mobileOnly,
          Fade(model.subProp(_.showActionPanelMobile),ClientConf.style.mobileBoxActionPanel){
            div(
              actions(metadata,nested,_.actions(model.subProp(_.write).get,presenter.roles())),
              nested(showIf(presenter.showNavigation) {
                div(actions(metadata,nested,_.navigation(model.subProp(_.write).get,presenter.roles()))).render
              }),
              button(ClientConf.style.boxIconButton, width := 100.pct, i(UdashIcons.FontAwesome.Solid.angleDown), onclick :+= ((e:Event) => model.subProp(_.showActionPanelMobile).set(false)))
            ).render
          }
      )

      ),
      nested(produce(model.subProp(_.error)){ error =>
        div(
          if(error.length > 0) {
            UdashBadge(badgeStyle = Color.Danger.toProperty)(_ => error).render
          } else {

          }
        ).render
      }),
      hr(ClientConf.style.hrThin)
    )

    def formFooter(metadata:JSONMetadata,nested:Binding.NestedInterceptor,_maxWidth:Option[Int]):Modifier = Seq(
      div(BootstrapCol.md(12),paddingTop := 10.px,ClientConf.style.margin0Auto,ClientConf.style.noMobile,id := "footerActions",
        _maxWidth.map(mw => maxWidth := mw),
        actions(metadata,nested,_.actions(model.subProp(_.write).get,presenter.roles())),
        ul(
         nested(produce(Notification.list){ notices =>
            notices.map { notice =>
              li(notice).render
            }
          }
        ))
      ),
      div(BootstrapCol.md(12),paddingTop := 10.px,ClientConf.style.margin0Auto,ClientConf.style.mobileOnly,ClientConf.style.mobileFooter,id := "footerActionsMobile",
        _maxWidth.map(mw => maxWidth := mw),
        actions(metadata,nested,_.actions(model.subProp(_.write).get,presenter.roles())),
        ul(
          nested(produce(Notification.list){ notices =>
            notices.map { notice =>
              li(notice).render
            }
          })
        )
      )
    ).render

    presenter.mainBinding = Some{produceWithNested(model.subProp(_.metadata)){ (_form,nested) =>



      div(nested(produceWithNested(model.subProp(_.write)){ (_,nested) => {

        val showHeader = _form.flatMap(_.params).forall(_.js(HIDE_HEADER) != Json.True)
        val showFooter = _form.flatMap(_.params).forall(_.js(HIDE_FOOTER) != Json.True)
        val showId = _form.flatMap(_.params).forall(_.js(HIDE_ID) != Json.True)
        val _maxWidth:Option[Int] = _form.flatMap(_.params.flatMap(_.js(MAX_WIDTH).as[Int].toOption))

        val stepperOptions = _form.flatMap(_.params).flatMap(_.js(STEPPER).as[StepperOption].toOption)

        div(
          if(showHeader && _form.isDefined) {
            formHeader(nested,showId,_form.get).render
          },
          div(BootstrapCol.md(12),if(showHeader) { ClientConf.style.fullHeightMaxForm },

            _form match {
              case None => div()
              case Some(f) => {
                val mainForm = form(
                  ClientConf.style.margin0Auto,
                  stepperOptions.map(stepper),
                  _maxWidth.map(mw => maxWidth := mw),
                  onsubmit :+= ((e:Event) => e.preventDefault()),
                  presenter.loadWidgets(f).render(model.get.write,nested)
                ).render
                presenter.setForm(mainForm)
                mainForm
              }
            },
            if(showFooter  && _form.isDefined) {
              formFooter(_form.get,nested,_maxWidth)
            }
          ).render,
          Debug(model.subProp(_.metadata),b => b, "metadata")

        ).render
      }})).render
    }}

    div(presenter.mainBinding)

  }
}
