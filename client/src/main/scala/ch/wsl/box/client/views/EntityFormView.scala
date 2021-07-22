package ch.wsl.box.client.views

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.{Context, FormState}
import ch.wsl.box.client.services.{ClientConf, Labels, Navigate, Navigation, Navigator, Notification}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.utils.HTMLFormElementExtension.HTMLFormElementExt
import ch.wsl.box.client.utils._
import ch.wsl.box.client.views.components.widget.Widget
import ch.wsl.box.client.views.components.{Debug, JSONMetadataRenderer}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.{Json, JsonNumber, JsonObject}
import io.udash.bootstrap.badge.UdashBadge
import io.udash.bootstrap.utils.BootstrapStyles.Color
import io.udash.{showIf, _}
import io.udash.bootstrap.{BootstrapStyles, UdashBootstrap}
import io.udash.component.ComponentId
import io.udash.core.Presenter
import io.udash.properties.single.Property
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, HTMLFormElement}
import scribe.Logging

import scala.concurrent.Future
import scalatags.JsDom
import scalacss.ScalatagsCss._
import scalacss.internal.StyleA

import scala.scalajs.js.URIUtils
import scala.language.reflectiveCalls

/**
  * Created by andre on 4/24/2017.
  */

case class EntityFormModel(name:String, kind:String, id:Option[String], metadata:Option[JSONMetadata], data:Json,
                           error:String, children:Seq[JSONMetadata], navigation: Navigation, loading:Boolean, changed:Boolean, write:Boolean, public:Boolean)

object EntityFormModel extends HasModelPropertyCreator[EntityFormModel] {
  implicit val blank: Blank[EntityFormModel] =
    Blank.Simple(EntityFormModel("","",None,None,Json.Null,"",Seq(), Navigation.empty0,true,false, true, false))
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

  private var currentData:Json = Json.Null

  override def handleState(state: FormState): Unit = {

    val reloadMetadata = {
      val currentModel = model.get

      !(currentModel.kind == state.kind &&
        currentModel.name == state.entity &&
        currentModel.metadata.isDefined)
    }

    model.subProp(_.kind).set(state.kind)
    model.subProp(_.name).set(state.entity)
    model.subProp(_.id).set(state.id)


    val jsonId = state.id.flatMap(JSONID.fromString)

    {for{
      metadata <- if(reloadMetadata) services.rest.metadata(state.kind, services.clientSession.lang(), state.entity,state.public) else Future.successful(model.get.metadata.get)
      children <- if(Seq(EntityKind.FORM,EntityKind.BOX).map(_.kind).contains(state.kind) && reloadMetadata) services.rest.children(state.kind,state.entity,services.clientSession.lang(),state.public) else Future.successful(Seq())
      data <- state.id match {
        case Some(id) => services.rest.get(state.kind, services.clientSession.lang(), state.entity,jsonId.get)
        case None => Future.successful{
          Json.obj(JSONMetadata.jsonPlaceholder(metadata,children).toSeq :_*)
        }
      }
    } yield {

      model.set(EntityFormModel(
        name = state.entity,
        kind = state.kind,
        id = state.id,
        metadata = Some(metadata),
        data = data,
        "",
        children,
        Navigation.empty1,
        true,
        false,
        state.writeable,
        state.public
      ))

      currentData = Json.Null.deepMerge(data)


      //need to be called after setting data because we are listening for data changes
      enableGoAway

      setNavigation()

      widget.afterRender()

      model.subProp(_.loading).set(false)

      TestHooks.loaded()

    }}.recover{ case e => e.printStackTrace() }

  }

  import io.circe.syntax._


  private var _form:HTMLFormElement = scalatags.JsDom.all.form().render

  def setForm(form:HTMLFormElement)= { _form = form }

  def save(action:JSONID => Unit):Unit  = {

    if(!_form.reportValidity()) {
      val errors = document.querySelectorAll("*:invalid")
      for(i <- 0 to errors.length) {
        errors.item(i) match {
          case e:HTMLElement => logger.warn(s"Error on: ${e.outerHTML}")
          case _ => logger.warn(s"Error on non HTMLElement")
        }
      }
      logger.warn(s"Form validation failed")
      return
    }

    val m = model.get
    m.metadata.foreach{ metadata =>
//      val jsons = for {
//        (field, i) <- form.fields.zipWithIndex
//      } yield Enhancer.parse(field, m.results.lift(i).map(_._2),form.keys){ t =>
//        model.subProp(_.error).set(s"Error parsing ${field.key} field: " + t.getMessage)
//      }

      val data:Json = m.data

      def saveAction(data:Json) = {
        logger.debug(s"saveAction id:${m.id} ${JSONID.fromString(m.id.getOrElse(""))}")
        for {
          id <- JSONID.fromString(m.id.getOrElse("")) match {
            case Some (id) => services.rest.update (m.kind, services.clientSession.lang(), m.name, id, data)
            case None => services.rest.insert (m.kind, services.clientSession.lang (), m.name, data,m.public)
          }
          result <- m.public match {
            case false => services.rest.get(m.kind, services.clientSession.lang(), m.name, id)
            case true => Future.successful(data)
          }
        } yield {
          logger.debug("saveAction::Result")
          (id,result)
        }

      }



      {for{
        updatedData <- widget.beforeSave(data,metadata)
        (newId,resultBeforeAfterSave) <- saveAction(updatedData)
        afterSaveResult <- widget.afterSave(resultBeforeAfterSave,metadata)


      } yield {

        logger.debug(afterSaveResult.toString())

        enableGoAway

        action(newId)


      }}.recover{ case e =>
        e.getStackTrace.foreach(x => logger.error(s"file ${x.getFileName}.${x.getMethodName}:${x.getLineNumber}"))
        e.printStackTrace()
      }
    }
  }

  def reload(id:JSONID): Unit = {
    for{
      resultSaved <- services.rest.get(model.get.kind, services.clientSession.lang(), model.get.name, id)
    } yield {
      reset()
      currentData = Json.Null.deepMerge(resultSaved)
      model.subProp(_.data).set(resultSaved)
      model.subProp(_.id).set(Some(id.asString), true)
      enableGoAway
      widget.afterRender()
    }
  }

  def revert() = {
      model.subProp(_.data).set(Json.Null.deepMerge(currentData))
      model.subProp(_.id).touch() //re-render childs
  }

  def delete() = {

      for{
        name <- model.get.metadata.map(_.name)
        key <- model.get.id.flatMap(JSONID.fromString)
      } yield {
        services.rest.delete(model.get.kind, services.clientSession.lang(),name,key).map{ count =>
          Notification.add("Deleted " + count.count + " rows")
          Navigate.to(Routes(model.get.kind, name).entity(name))
        }
      }

  }

  def reset(): Unit = {
    model.subProp(_.data).set(Json.Null)
    model.subProp(_.id).set(None)
    enableGoAway
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
    services.navigator.For(model.get.id).navigation().map{ nav =>
      model.subProp(_.navigation).set(nav)
    }
  }

  private var widget:Widget = new Widget {
    import scalatags.JsDom.all._

    override def field: JSONField = JSONField.empty

    override protected def show(): JsDom.all.Modifier = div()
    override protected def edit(): JsDom.all.Modifier = div()
  }

  def loadWidgets(f:JSONMetadata) = {
    widget = JSONMetadataRenderer(f, model.subProp(_.data), model.subProp(_.children).get, model.subProp(_.id))
    widget
  }



  def navigate(n: navigator.For => Future[Option[String]]) = {
    n(nav).map(_.map(goTo))
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

  def nav = navigator.For(model.get.id, model.get.kind,model.get.name)

  def goTo(id:String) = {
    model.subProp(_.loading).set(true)
    val m = model.get
    val r = Routes(m.kind,m.name)
    val newState = if(model.get.write) {
      r.edit(id)
    } else {
      r.show(id)
    }
    Navigate.to(newState)
  }


  def removeNonDataFields(js:Json):Json = {

    val folder = new Json.Folder[Json] {
      override def onNull: Json = Json.Null
      override def onBoolean(value: Boolean): Json = Json.fromBoolean(value)
      override def onNumber(value: JsonNumber): Json = Json.fromJsonNumber(value)
      override def onString(value: String): Json = Json.fromString(value)
      override def onArray(value: Vector[Json]): Json = Json.fromValues(value.map(removeNonDataFields))
      override def onObject(value: JsonObject): Json = Json.fromJsonObject{
        value.filter(!_._1.startsWith("$"))
      }
    }

    Json.Null.deepMerge(js).foldWith(folder).deepDropNullValues

  }

  model.subProp(_.data).listen { d =>
    if(!currentData.equals(removeNonDataFields(d))) {
      logger.info(currentData.toString())
      logger.info(d.toString())
      avoidGoAway
    } else {
      enableGoAway
    }
  }

  def avoidGoAway = {
    Navigate.disable{ () =>
      window.confirm(Labels.navigation.goAway)
    }
    model.subProp(_.changed).set(true)
    window.onbeforeunload = { (e:BeforeUnloadEvent) =>
      Labels.navigation.goAway
    }
  }
  def enableGoAway = {
    Navigate.enable()
    model.subProp(_.changed).set(false)
    window.onbeforeunload = { (e:BeforeUnloadEvent) =>
      widget.killWidget()
    }
  }

  val showNavigation:ReadableProperty[Boolean] = model.subProp(_.public).transform(x => !x)
    .combine {
      model.subProp(_.metadata).transform(x => !x.exists(_.static))
    }(_ && _)
    .combine {
      model.subProp(_.metadata).transform(x => x.exists(_.action.showNavigation))
    }(_ && _)



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

      def callBack() = action.action match {
        case SaveAction => presenter.save{ _id =>
          if(action.reload) {
            presenter.reload(_id)
          }
          action.getUrl(model.get.kind,model.get.name,Some(_id.asString),model.get.write).foreach{ url =>
            presenter.reset()
            Navigate.toUrl(url)
          }
        }
        case NoAction => action.getUrl(model.get.kind,model.get.name,_id,model.get.write).foreach{ url =>
          presenter.reset()
          Navigate.toUrl(url)
        }
        case CopyAction => presenter.duplicate()
        case DeleteAction => presenter.delete()
        case RevertAction => presenter.revert()
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

    if((action.updateOnly && _id.isDefined) || (action.insertOnly && _id.isEmpty) || (!action.insertOnly && !action.updateOnly)) {
      button(
        id := TestHooks.actionButton(action.label),
        importance,
        onclick :+= ((ev: Event) => {
          confirm(callBack)
          ev.preventDefault()
        })
      )(Labels(action.label)).render
    } else frag()

  }

  override def getTemplate: scalatags.generic.Modifier[Element] = {

    val recordNavigation = showIf(presenter.showNavigation){

          def navigation = model.subModel(_.navigation)

          div(
            div(ClientConf.style.boxNavigationLabel,
              Navigation.button(navigation.subProp(_.hasPreviousPage), presenter.firstPage, Labels.navigation.firstPage, _.Float.left()),
              Navigation.button(navigation.subProp(_.hasPreviousPage), presenter.prevPage, Labels.navigation.previousPage, _.Float.left()),
              span(
                ClientConf.style.boxNavigationLabel,
                " " + Labels.navigation.page + " ",
                bind(model.subProp(_.navigation.currentPage)),
                " " + Labels.navigation.of + " ",
                bind(model.subProp(_.navigation.pages)),
                " "
              ),
              Navigation.button(navigation.subProp(_.hasNextPage), presenter.lastPage, Labels.navigation.lastPage, _.Float.right()),
              Navigation.button(navigation.subProp(_.hasNextPage), presenter.nextPage, Labels.navigation.nextPage, _.Float.right())
            ),
            div(BootstrapStyles.Visibility.clearfix),
            div(ClientConf.style.boxNavigationLabel,
              Navigation.button(navigation.subProp(_.hasPrevious), presenter.first, Labels.navigation.first, _.Float.left()),
              Navigation.button(navigation.subProp(_.hasPrevious), presenter.prev, Labels.navigation.previous, _.Float.left()),
              span(
                " " + Labels.navigation.record + " ",
                bind(model.subModel(_.navigation).subProp(_.currentIndex)),
                " " + Labels.navigation.of + " ",
                bind(model.subModel(_.navigation).subProp(_.count)),
                " "
              ),
              Navigation.button(navigation.subProp(_.hasNext), presenter.last, Labels.navigation.last, _.Float.right()),
              Navigation.button(navigation.subProp(_.hasNext), presenter.next, Labels.navigation.next, _.Float.right())
            )
          ).render
    }

    def actions = div(
      produceWithNested(model.subProp(_.write)) { (w,realeser) =>
        if(!w) Seq() else
        div(BootstrapStyles.Float.left())(
          realeser(produceWithNested(model.subProp(_.metadata)) { (form,realeser2) =>
            div(
              realeser2(produce(model.subProp(_.id)) { _id =>
                div(
                  form.toSeq.flatMap(_.action.actions).map(actionRenderer(_id))
                ).render
              })
            ).render
          })
        ).render
      },
      div(BootstrapStyles.Visibility.clearfix)
    )

    val formHeader = div(
      div(BootstrapStyles.Float.left(),
        h3(
          ClientConf.style.noMargin,
          labelTitle,
          showIf(model.subProp(_.metadata).transform(!_.exists(_.static))) {
            small(produce(model.subProp(_.id)) { id =>
              val subTitle = id.map(" - " + _).getOrElse("")
              span(subTitle).render
            }).render
          },
          showIf(model.subProp(_.loading)) {
            small(" - " + Labels.navigation.loading).render
          },
          showIf(model.subProp(_.changed)) {
            small(id := TestHooks.dataChanged,style := "color: red"," - " + Labels.form.changed).render
          }

        )
      ),
      showIf(model.transform(_.navigation.count > 1)) {
        div(BootstrapStyles.Float.right(), ClientConf.style.navigatorArea)(
          recordNavigation
        ).render
      } ,
      showIf(presenter.showNavigation) {
        div(BootstrapStyles.Float.right(), ClientConf.style.navigatorArea)(
          produceWithNested(model.subProp(_.name)) { (m, release) =>
            div(
              button(ClientConf.style.boxButton, Navigate.click(Routes(model.subProp(_.kind).get, m).entity(m)))(Labels.entities.table + " ", release(labelTitle)), " "
            ).render
          }
        ).render
      },
      div(BootstrapStyles.Visibility.clearfix),
      actions,
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

    val formFooter = div(BootstrapCol.md(12),marginTop := 10.px,
      actions
    )

    val showHeader = model.subProp(_.metadata).transform(_.flatMap(_.params).forall(_.js("hideHeader") != Json.True))

    val showFooter = model.subProp(_.metadata).transform(_.flatMap(_.params).forall(_.js("hideFooter") != Json.True))

    div(
      showIf(showHeader) {
        formHeader.render
      },
      produce(model.subProp(_.metadata)){ _form =>
        div(BootstrapCol.md(12),ClientConf.style.fullHeightMax,
          _form match {
            case None => p("Loading form")
            case Some(f) => {
              val mainForm = form(
                presenter.loadWidgets(f).render(model.get.write,Property(true))
              ).render
              presenter.setForm(mainForm)
              mainForm
            }
          },
          showIf(showFooter) {
            formFooter.render
          }
        ).render
      },
      Debug(model.subProp(_.data),b => b, "data"),
      Debug(model.subProp(_.metadata),b => b, "metadata")
    )
  }
}
