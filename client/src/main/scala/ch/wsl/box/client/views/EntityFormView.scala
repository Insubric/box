package ch.wsl.box.client.views

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.{Context, EntityFormState, EntityTableState, FormState}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels, Navigate, Navigation, Navigator, Notification}
import ch.wsl.box.client.styles.{BootstrapCol, Fade}
import ch.wsl.box.client.utils.HTMLFormElementExtension.HTMLFormElementExt
import ch.wsl.box.client.utils._
import ch.wsl.box.client.views.components.ui.Stepper
import ch.wsl.box.client.views.components.widget.{Widget, WidgetCallbackActions}
import ch.wsl.box.client.views.components.{Debug, JSONMetadataRenderer}
import ch.wsl.box.model.shared._
import ch.wsl.box.model.shared.errors.SQLExceptionReport
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

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.URIUtils
import scala.language.reflectiveCalls
import scala.scalajs.js.timers.setTimeout
import scala.util.{Failure, Success, Try}

/**
  * Created by andre on 4/24/2017.
  */

case class EntityFormModel(name:String, kind:String, id:Option[String], metadata:Option[JSONMetadata], originalData:Json,data:Json,
                           error:String, children:Seq[JSONMetadata], navigation: Navigation, changed:Boolean, write:Boolean, public:Boolean, insert:Boolean, showActionPanelMobile: Boolean)

object EntityFormModel extends HasModelPropertyCreator[EntityFormModel] {

  val empty = EntityFormModel("","",None,None,Json.Null,Json.Null,"",Seq(), Navigation.empty0,false, true, false, true, false)

  implicit val blank: Blank[EntityFormModel] = Blank.Simple(empty)
}

object EntityFormViewPresenter extends ViewFactory[FormState] {

  override def create(): (View, Presenter[FormState]) = {
    val model = ModelProperty.blank[EntityFormModel]
    val presenter = EntityFormPresenter(model)
    (EntityFormView(model,presenter),presenter)
  }
}

case class EntityFormPresenter(model:ModelProperty[EntityFormModel]) extends Presenter[FormState] with Logging {
  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  override def handleState(state: FormState): Unit = {

    logger.warn(state.toString)

    val loaded = Promise[Boolean]()
    TestHooks.addLoadedPromise(loaded)

    services.clientSession.loading.set(true)

    model.subProp(_.kind).set(state.kind)
    model.subProp(_.name).set(state.entity)
    model.subProp(_.id).set(state.id)
    model.subProp(_.insert).set(state.id.isEmpty)


    {for{
      metadata <- services.rest.metadata(state.kind, services.clientSession.lang(), state.entity,state.public)
      children <- if(Seq(EntityKind.FORM,EntityKind.BOX_FORM).map(_.kind).contains(state.kind)) services.rest.children(state.kind,state.entity,services.clientSession.lang(),state.public) else Future.successful(Seq())
      data <- state.id match {
        case Some(id) => {
          val jsonId = state.id.flatMap(x => JSONID.fromString(x,metadata)) match {
            case Some(value) => value
            case None => throw new Exception(s"cannot parse JsonID ${state.id}")
          }
          services.rest.get(state.kind, services.clientSession.lang(), state.entity,jsonId,state.public)
        }
        case None => Future.successful(Json.Null)
      }
    } yield {

      BrowserConsole.log(data)

      var insert = false

      //check if data is already present for the given id
      val dataWithId = if(data == Json.Null) { // if not we are going to do an insert
        insert = true
        val d = Json.obj(JSONMetadata.jsonPlaceholder(metadata,children).toSeq :_*) // taking the defaults
        state.id.flatMap(JSONID.fromString(_, metadata)) match {
          case Some(id) => d.deepMerge(Json.fromFields(id.toFields))
          case None => d
        }
      } else {
        data
      }

      val dataWithQueryParams = dataWithId.deepMerge(Json.fromFields(Routes.urlParams.toSeq.map(x => x._1 -> Json.fromString(x._2))))

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
        state.writeable,
        state.public,
        insert,
        false
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

  val saveKey:KeyHandler = (event:KeyboardEvent,handler:HotkeysEvent) => {
    event.preventDefault()
    save().map{ case (id,d) => afterSave(id,d)}
    false
  }

  ch.wsl.typings.hotkeysJs.mod.default("ctrl+s",saveKey)

  override def onClose(): Unit = {
    if(Navigate.canGoAway) {
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
    }

  }

  private var _form:HTMLFormElement = scalatags.JsDom.all.form().render

  def setForm(form:HTMLFormElement)= { _form = form }


  def save(check:Boolean = true):Future[(JSONID,Json)]  = {

    services.clientSession.loading.set(true)

    if(check) if(!_form.reportValidity()) {
      services.clientSession.loading.set(false)
      val errors = document.querySelectorAll("*:invalid")
      for(i <- 0 to errors.length) {
        errors.item(i) match {
          case e:HTMLElement => logger.warn(s"Error on: ${e.outerHTML}")
          case _ => logger.warn(s"Error on non HTMLElement")
        }
      }
      logger.warn(s"Form validation failed")

      return Future.failed(new Exception("Validation failed"))
    }

    val m = model.get
    val metadata = m.metadata.get
    val originalId = JSONID.fromData(m.originalData,metadata)
    val data:Json = m.data

    def saveAction(data:Json):Future[(JSONID,Json)] = {

      logger.info(s"saveAction id:$originalId ${JSONID.fromString(m.id.getOrElse(""),metadata)}")
      for {
        result <- JSONID.fromString(m.id.getOrElse(""),metadata) match {
          case Some(id) if !model.subProp(_.insert).get => services.rest.update (m.kind, services.clientSession.lang(), m.name, originalId.getOrElse(id), data,m.public)
          case _ => services.rest.insert (m.kind, services.clientSession.lang (), m.name, data,m.public)
        }
      } yield {
        logger.debug(s"saveAction::Result")
        (JSONID.fromData(result,metadata,false).getOrElse(JSONID.empty),result)
      }

    }



    {for{
      updatedData <- widget.beforeSave(data,metadata)
      (newId,resultAfterAction) <- saveAction(updatedData.removeNonDataFields(metadata,m.children))

    } yield {
      logger.debug(s"""outcome

                 Save outcome:

                 original: $data

                 afterBeforeSave: $updatedData

                 afterSave: $resultAfterAction


                 """)

      enableGoAway("save")
      val currentState = model.get

      val newData = currentState.data.deepMerge(resultAfterAction)



      (newId,newData)


    }}.recover{ case e =>
      e.getStackTrace.foreach(x => logger.error(s"file ${x.getFileName}.${x.getMethodName}:${x.getLineNumber}"))
      e.printStackTrace()
      services.clientSession.loading.set(false)
      throw e
    }

  }

  def afterSave(id:JSONID,data:Json) = {
    val currentState = model.get
    model.set(currentState.copy(
      data = data,
      originalData = data,
      id = Some(id.asString),
      insert = false,
      changed = false
    ))
    childChanged.set(false)
    resetChanges()

    services.clientSession.loading.set(false)
  }

  def reload(id:JSONID): Future[Json] = {
    services.clientSession.loading.set(true)
    for{
      resultSaved <- services.rest.get(model.get.kind, services.clientSession.lang(), model.get.name, id,model.get.public)
      result <- {
        val promise = Promise[Json]()
        reset()

        model.set(model.get.copy(
          data = resultSaved,
          originalData = resultSaved,
          id = Some(id.asString)
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

  def revert() = {
    model.subProp(_.id).get.flatMap(x => JSONID.fromString(x,model.get.metadata.get)) match {
      case Some(id) => reload(id)
      case None => logger.warn("Cannot revert with no ID")
    }
  }

  def delete() = {

      for{
        name <- model.get.metadata.map(_.name)
        key <- model.get.id.flatMap(x => JSONID.fromString(x,model.get.metadata.get))
      } yield {
        services.rest.delete(model.get.kind, services.clientSession.lang(),name,key).map{ count =>
          Notification.add("Deleted " + count.count + " rows")
          Navigate.to(Routes(model.get.kind, name,model.subProp(_.public).get).entity(name))
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
    save().map { case (id, d) => afterSave(id, d) }
  }

  val changesListener = childChanged.listen { hasChanges =>
    if(hasChanges) {
      avoidGoAway
      if(autoSave)
        debounceSave()

    } else {
      enableGoAway("changeListener")
    }
  }

  def loadWidgets(f:JSONMetadata) = {
    val actions = WidgetCallbackActions((f:(JSONID,Json) => Future[Unit]) => save().foreach{ case (id,data) =>
      f(id,data).foreach{ _ =>
        afterSave(id,data)
      }
    },reload)
    widget = JSONMetadataRenderer(f, model.subProp(_.data), model.subProp(_.children).get, model.subProp(_.id),actions,childChanged,model.subProp(_.public).get)
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
    window.onbeforeunload = { (e:BeforeUnloadEvent) =>
      if(Context.applicationInstance.currentState.isInstanceOf[EntityFormState] || Context.applicationInstance.currentState.isInstanceOf[EntityTableState]) {
        e.preventDefault()
        Labels.navigation.goAway
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


  def actionClick(_id:Option[String],action:FormAction):Event => Any  = {

    def afterGoto(url:String) = {
      action.target match {
        case Self => Navigate.toUrl(url)
        case NewWindow => {
          window.open(url)
        }
      }
    }

    def executeFunction():Future[Option[Boolean]] = action.executeFunction match {
      case Some(value) => services.rest.execute(value,services.clientSession.lang(),model.get.data).map{ result =>
        result.errorMessage match {
          case Some(value) => {
            Notification.add(value)
            services.clientSession.loading.set(false)
            Some(false)
          }
          case None => Some(true)
        }
      }
      case None => Future.successful(None)
    }

    def callBack() = action.action match {
      case SaveAction =>  save(action.html5check).map{ case (_id,data) =>

        def onSuccess = Routes.getUrl(action, model.get.data, model.get.kind, model.get.name, Some(_id.asString), model.get.write) match {
          case Some(url) => {
            logger.warn(s"Navigating to $url")
            //reset()
            afterGoto(url)
          }
          case None => afterSave(_id,data)
        }

        executeFunction().map {
          case Some(true) => {
            if (action.reload) {
              reload(_id)
            }
            onSuccess
          }
          case None => onSuccess
          case Some(false) => afterSave(_id,data)

        }
      }
      case NoAction => Routes.getUrl(action,model.get.data,model.get.kind,model.get.name,_id,model.get.write).foreach{ url =>
        executeFunction().map {
          case Some(true) => {
            if (Navigate.canGoAway)
              reset()
            afterGoto(url)
          }
          case None => afterGoto(url)
          case Some(false) => ()

        }
      }
      case CopyAction => duplicate()
      case DeleteAction => delete()
      case RevertAction => revert()
      case BackAction => Navigate.back()

    }

    def confirm(cb: () => Any) =  action.confirmText match {
      case Some(ct) => {
        val confim = window.confirm(Labels(ct))
        if(confim) {
          cb()
        }
      }
      case None => cb()
    }



    (ev: Event) => {
      logger.info(s"Execution action $action")
      model.subProp(_.showActionPanelMobile).set(false)
      confirm(callBack)
      ev.preventDefault()
    }
  }

}

case class EntityFormView(model:ModelProperty[EntityFormModel], presenter:EntityFormPresenter) extends View {
  import scalatags.JsDom.all._
  import io.circe.generic.auto._
  import io.udash.css.CssView._


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

  def actionRenderer(_id:Option[String])(action:FormAction):Modifier = {

      val importance:StyleA = action.importance match {
        case Primary => ClientConf.style.boxButtonImportant
        case Danger => ClientConf.style.boxButtonDanger
        case Std => ClientConf.style.boxButton
      }


    if((action.updateOnly && _id.isDefined && !model.subProp(_.insert).get) || (action.insertOnly && model.subProp(_.insert).get) || (!action.insertOnly && !action.updateOnly)) {
      val actionButton = button(
        id := TestHooks.actionButton(action.label),
        importance,
        onclick :+= presenter.actionClick(_id,action)
      )(Labels(action.label)).render
      action.condition match {
        case Some(conditions) => {
          showIf(model.subProp(_.data).transform{ js => conditions.forall{ cond =>
            cond.check(js)
          }})( actionButton )
        }
        case None => actionButton
      }
    } else frag()

  }

  case class StepperOption(steps:Seq[String],current:String)


  def stepper(s:StepperOption) = {
    div(margin := "20px",
      Stepper.render(s.steps.map(st => Stepper.Step(st,"","")),s.steps.indexOf(s.current))
    )
  }

  override def getTemplate: scalatags.generic.Modifier[Element] = {

    def recordNavigation = showIf(presenter.showNavigation){

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

    def actions(selector:FormActionsMetadata => Seq[FormAction]) = div(
      produceWithNested(model.subProp(_.write)) { (w,realeser) =>
        if(!w) Seq() else
        div(
          realeser(produceWithNested(model.subProp(_.metadata)) { (form,realeser2) =>
            div(
              realeser2(produceWithNested(model.subProp(_.id)) { case (_id,releaser3) =>
                div(ClientConf.style.spaceBetween,
                  releaser3(produce(model.subProp(_.insert)) { _ =>
                    div(
                      form.toSeq.flatMap(f => selector(f.action)).map(actionRenderer(_id))
                    ).render
                  })
                ).render
              })
            ).render
          })
        ).render
      },
      div(BootstrapStyles.Visibility.clearfix)
    )

    def formHeader(showId:Boolean,metadata:JSONMetadata) = div(ClientConf.style.formHeader,
      div( ClientConf.style.spaceBetween, marginBottom := 10.px,
        h3(
          ClientConf.style.noMargin,
          ClientConf.style.formTitle,
          labelTitle
        ),
        div(
          showIf(model.subProp(_.changed)) {
            small(id := TestHooks.dataChanged,style := "color: red",Labels.form.changed).render
          }
        ),
        div(
          ClientConf.style.noMargin,
          ClientConf.style.formTitle,
          if(showId) {
            showIf(model.subProp(_.metadata).transform(!_.exists(_.static))) {
              div(produce(model.subProp(_.id)) { id =>
                id.flatMap(JSONID.fromString(_,metadata)).toSeq.flatMap{ jsonId =>
                  jsonId.id.map{ k =>
                    val label = metadata.fields.find(_.name == k.key).flatMap(_.label).getOrElse(k.key)
                    span(span(ClientConf.style.formTitleLight,label), " ",k.value.string, " ").render
                  }
                }
              }).render
            }
          } else frag(),
        )
      ),
//      div(ClientConf.style.mobileOnly,
//        button(ClientConf.style.boxButton,i(UdashIcons.FontAwesome.Solid.ellipsisV))
//      ),
      div(ClientConf.style.spaceBetween,ClientConf.style.noMobile,
        actions(_.actions),
        div(ClientConf.style.spaceAfter)(
          showIf(presenter.showNavigation) {
            div(actions(_.navigationActions)).render
          },
          showIf(model.transform(_.navigation.count > 1)) {
              div(recordNavigation).render
          }
        ).render,
      ),
      div(ClientConf.style.mobileOnly,
        showIf(model.transform(_.navigation.count > 1)) {
          div(ClientConf.style.spaceBetween,recordNavigation).render
        },
        button(ClientConf.style.mobileBoxAction)(i(UdashIcons.FontAwesome.Solid.pen), onclick :+= ((e:Event) => model.subProp(_.showActionPanelMobile).set(true))).render,
        div(ClientConf.style.mobileOnly,
          Fade(model.subProp(_.showActionPanelMobile),ClientConf.style.mobileBoxActionPanel){
            div(
              actions(_.actions),
              showIf(presenter.showNavigation) {
                div(actions(_.navigationActions)).render
              },
              button(ClientConf.style.boxIconButton, width := 100.pct, i(UdashIcons.FontAwesome.Solid.angleDown), onclick :+= ((e:Event) => model.subProp(_.showActionPanelMobile).set(false)))
            ).render
          }
      )

      ),
      produce(model.subProp(_.error)){ error =>
        div(
          if(error.length > 0) {
            UdashBadge(badgeStyle = Color.Danger.toProperty)(_ => error).render
          } else {

          }
        ).render
      },
      hr(ClientConf.style.hrThin)
    )

    def formFooter(_maxWidth:Option[Int]):Modifier = Seq(
      div(BootstrapCol.md(12),paddingTop := 10.px,ClientConf.style.margin0Auto,ClientConf.style.noMobile,id := "footerActions",
        _maxWidth.map(mw => maxWidth := mw),
        actions(_.actions),
        ul(
          produce(Notification.list){ notices =>
            notices.map { notice =>
              li(notice).render
            }
          }
        )
      ),
      div(BootstrapCol.md(12),paddingTop := 10.px,ClientConf.style.margin0Auto,ClientConf.style.mobileOnly,ClientConf.style.mobileFooter,id := "footerActionsMobile",
        _maxWidth.map(mw => maxWidth := mw),
        actions(_.actions),
        ul(
          produce(Notification.list){ notices =>
            notices.map { notice =>
              li(notice).render
            }
          }
        )
      )
    ).render



    div(
      produceWithNested(model.subProp(_.metadata)){ (_form,nested) =>

        val showHeader = _form.flatMap(_.params).forall(_.js("hideHeader") != Json.True)
        val showFooter = _form.flatMap(_.params).forall(_.js("hideFooter") != Json.True)
        val showId = _form.flatMap(_.params).forall(_.js("hideID") != Json.True)
        val _maxWidth:Option[Int] = _form.flatMap(_.params.flatMap(_.js("maxWidth").as[Int].toOption))

        val stepperOptions = _form.flatMap(_.params).flatMap(_.js("stepper").as[StepperOption].toOption)

        div(
          if(showHeader && _form.isDefined) {
            formHeader(showId,_form.get).render
          },
          div(BootstrapCol.md(12),if(showHeader) { ClientConf.style.fullHeightMax },

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
            if(showFooter) {
              formFooter(_maxWidth)
            }
          ).render,
          Debug(model.subProp(_.metadata),b => b, "metadata")

        ).render
      }
    )
  }
}
