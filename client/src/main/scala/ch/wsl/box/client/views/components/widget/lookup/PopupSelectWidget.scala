package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.JSONMetadataRenderer
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetCallbackActions, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.properties.seq
import io.udash.properties.single.Property
import org.scalajs.dom.{Event, HTMLInputElement, document}
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.duration._


sealed trait Mode
case object Edit extends Mode
case object Search extends Mode

object PopupSelectWidget extends ComponentWidgetFactory  {

  override def name: String = WidgetsNames.popup

  override def create(params: WidgetParams): Widget = PopupWidget(params.field,params.prop,params.allData,params.metadata,params.public)


  case class PopupWidget(field:JSONField, data: Property[Json],allData:ReadableProperty[Json],metadata:JSONMetadata,public:Boolean) extends LookupWidget with Logging {

    import io.udash.css.CssView._
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(WidgetUtils.showNotNull(data,nested){ _ =>

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin)(
        label(field.title),
        div(BootstrapStyles.Float.right(), ClientConf.style.popupButton,
          bind(model.transform(_.map(_.value).getOrElse("")))
        ),
        div(BootstrapStyles.Visibility.clearfix)
      ).render
    })

    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    import ch.wsl.box.client.Context.Implicits._


    val mode:Property[Mode] = Property(Search)
    val lookupData = Property(Json.obj())

    def popupEdit(nested:Binding.NestedInterceptor)(mainRenderer:(UdashModal,Property[String]) => Modifier) = {

      val searchId = TestHooks.popupSearch(field.name,metadata.objId)

      val searchProp = Property("")

      val modalStatus = Property(Status.Closed)

      def optionList(nested:NestedInterceptor):Modifier = div(
        label(Labels.popup.search),br,
        TextInput(searchProp,500.milliseconds)(width := 100.pct, id := searchId),br,br,
        button(`type` := "button", Icons.plus, " ", Labels.entities.`new`,ClientConf.style.boxButtonImportant,onclick :+= ((e:Event) => {
          lookupData.set(Json.obj())
          mode.set(Edit)
        })),br,br,
        nested(showIf(modalStatus.transform(_ == Status.Open)) {
          div(ClientConf.style.popupEntiresList,nested(produce(searchProp) { searchTerm =>
            div(
              div(
                nested(repeat(lookup.filter(opt => searchTerm == "" || opt.value.toLowerCase.contains(searchTerm.toLowerCase))) { x =>
                  div(
                    a(Icons.pencil_square,onclick :+= ((e: Event) => {
                      val lookup = fieldLookup.asInstanceOf[JSONFieldLookupRemote]
                      val keys = Seq((lookup.map.foreign.keyColumns.head,x.get.id)) // single lookup supported
                      //val keys = lookup.map.localKeysColumn.zip(lookup.map.foreign.keyColumns).map{ case (local,remote) => remote -> x.get.id}
                      services.rest.get(EntityKind.ENTITY.kind,services.clientSession.lang(),lookup.lookupEntity,JSONID.fromMap(keys),public).map { record =>
                        lookupData.set(record)
                        mode.set(Edit)
                      }
                      e.preventDefault()
                    })), " ",
                    a(bind(x.transform(_.value)), onclick :+= ((e: Event) => {
                      modalStatus.set(Status.Closed)
                      model.set(Some(x.get))
                      e.preventDefault()
                    })
                  )).render
                }
                )
              )
            ).render
          })).render
        }
      ))

      def editEntity(entity:String,nested:NestedInterceptor):Modifier = {



        val metadata:Property[Option[JSONMetadata]] = Property(None)

        services.rest.metadata(EntityKind.ENTITY.kind,services.clientSession.lang(),entity,public).foreach(m => metadata.set(Some(m)))

        nested(produceWithNested(metadata) { (metadata,nested) =>
          div(metadata.map { metadata =>
            val id = JSONID.fromData(lookupData.get, metadata)
            val action = WidgetCallbackActions.noAction
            JSONMetadataRenderer(metadata, lookupData, Seq(), Property(id.map(_.asString)), action, Property(false), public).edit(nested)
          }).render
        })
      }

      def editEntry(nested:NestedInterceptor):Modifier = {
        fieldLookup match {
          case JSONFieldLookupRemote(lookupEntity, map, lookupQuery) => editEntity(lookupEntity,nested)
          case JSONFieldLookupExtractor(extractor) => ???
          case JSONFieldLookupData(data) => ???
        }
      }

      var modal:UdashModal = null

      val header = (n:NestedInterceptor) => div(
        b(field.title),
        div(width := 100.pct, textAlign.center,bind(model.transform(_.map(_.value).getOrElse("")))),
        n(UdashButton()( _ => Seq[Modifier](
          onclick :+= {(e:Event) => modalStatus.set(Status.Closed); e.preventDefault()},
          BootstrapStyles.close, "×"
        ))).render
      ).render

      val body = (x:NestedInterceptor) => div(
          x(produceWithNested(mode) { (m,nested) =>
            m match {
              case Edit => div(editEntry(nested)).render
              case Search => div(optionList(nested)).render
            }
          })
      ).render

      val footer = (nested:NestedInterceptor) => div(
        nested(showIf(model.transform(_.isDefined)) {
          button(onclick :+= ((e: Event) => {
            model.set(None)
            modal.hide()
            e.preventDefault()
          }), Labels.popup.remove, ClientConf.style.boxButtonDanger).render
        }),
        button(onclick :+= ((e:Event) => {
          modal.hide()
          e.preventDefault()
        }), Labels.popup.close,ClientConf.style.boxButton)
      ).render

      modal = nested(UdashModal(modalSize = Some(Size.Small).toProperty)(
        headerFactory = Some(header),
        bodyFactory = Some(body),
        footerFactory = Some(footer)
      ))



      modal.listen { case ev:ModalEvent =>
        ev.tpe match {
          case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
          case ModalEvent.EventType.Shown => {
            mode.set(Search)
            document.getElementById(searchId).asInstanceOf[HTMLInputElement].focus()
          }
          case _ => {}
        }
      }

      modalStatus.listen{ state =>
        logger.info(s"State changed to:$state")
        state match {
          case Status.Open => modal.show()
          case Status.Closed => modal.hide()
        }
      }

      mainRenderer(modal,modalStatus)

    }

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = popupEdit(nested)((modal,modalStatus) => {
      div(
        TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
        button(ClientConf.style.popupButton, width := 100.pct, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        }),
          bind(model.transform(_.map(_.value).getOrElse("")))
        ),
        modal.render
      )
    })

    override def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = popupEdit(nested)((modal,modalStatus) => {
      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin,
        BootstrapStyles.Display.flex(),BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Between))(
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
        tooltip(button(ClientConf.style.popupButton, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        }),nested(bind(model.transform(_.map(_.value).getOrElse(""))))).render)._1,
        modal.render

      )
    })
  }


}
