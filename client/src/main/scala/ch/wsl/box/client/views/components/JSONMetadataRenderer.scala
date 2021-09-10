package ch.wsl.box.client.views.components


import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.views.components
import ch.wsl.box.client.views.components.widget._
import ch.wsl.box.client.views.components.widget.child.ChildRenderer
import ch.wsl.box.client.views.components.widget.labels.{StaticTextWidget, TitleWidget}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.form.UdashForm
import io.udash._
import io.udash.bootstrap.tooltip.UdashTooltip

import scala.concurrent.Future
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.bindings.modifiers.Binding
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import org.scalajs.dom.Event
/**
  * Created by andre on 4/25/2017.
  */


case class JSONMetadataRenderer(metadata: JSONMetadata, data: Property[Json], children: Seq[JSONMetadata], id: Property[Option[String]],actions: WidgetCallbackActions,changed:Property[Boolean]) extends ChildWidget  {


  import ch.wsl.box.client.Context._
  import io.circe._

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  private val currentData:Property[Json] = Property(data.get)

  def resetChanges() = {
    logger.info(s"${metadata.name} resetChanges")
    blocks.foreach(_._2.resetChangeAlert())
    currentData.set(data.get)
  }

  data.listen { d =>
    logger.info(s"changed ${d.js(ChildRenderer.CHANGED_KEY)} on ${metadata.name}")
    if(d.js(ChildRenderer.CHANGED_KEY) == Json.True) {
      changed.set(true,true)
      logger.info(s"${metadata.name} has changes in child")
    } else if(!currentData.get.removeNonDataFields.equals(d.removeNonDataFields)) {
      changed.set(true,true)
      logger.info(s"""
                ${metadata.name} has changes

                original:
                ${currentData.get.removeNonDataFields}

                new:
                ${d.removeNonDataFields}

                """)

    } else {
      changed.set(false,true)
    }
  }


  override def field: JSONField = JSONField("metadataRenderer","metadataRenderer",false)

  private def getId(data:Json): Option[String] = {
    if(metadata.static) id.get
    else
      data.ID(metadata.keys).map(_.asString)
  }


  data.listen { data =>
    val currentID = getId(data)
    if (currentID != id.get) {
      id.set(currentID)
    }
  }

  private def checkCondition(field: JSONField) = {
    field.condition match {
      case None => Property(true)
      case Some(condition) => {

        val observedData = Property(data.get.js(condition.conditionFieldId))


        data.listen{ d =>
          val newJs = d.js(condition.conditionFieldId)
          if( newJs != observedData.get) {
            observedData.set(newJs)
          }
        }

        def evaluate(d:Json):Boolean = {
          val value = d
          val r = condition.conditionValues.contains(value)
          logger.info(s"evaluating condition for field: ${field.name} against $value with accepted values: ${condition.conditionValues} with result: $r")
          r
        }


        val visibility = Property(false)
        observedData.listen(d => {
          val r = evaluate(d)
          if(r == !visibility.get) { //change only when the status changesW
            visibility.set(r)
          }
        },true)
        visibility
      }
    }
  }


  private def widgetSelector(field: JSONField, id:Property[Option[String]], fieldData:Property[Json]): Widget = {



    val _field:JSONField = WidgetUtils.isKeyNotEditable(metadata,field,id.get) match {
      case true => field.copy(readOnly = true)
      case false => field
    }

    val widg = field.widget match {
      case Some(value) => WidgetRegistry.forName(value)
      case None => WidgetRegistry.forType(field.`type`)
    }

    logger.debug(s"Selected widget for ${field.name}: ${widg}")

    widg.create(WidgetParams(id,fieldData,_field,metadata,data,children,actions))

  }





  case class WidgetVisibility(widget:Widget,visibility: ReadableProperty[Boolean])

  object WidgetVisibility{
    def apply(widget: Widget): WidgetVisibility = WidgetVisibility(widget, Property(true))
  }



  private def subBlock(block: SubLayoutBlock):WidgetVisibility = WidgetVisibility(new Widget {

    val widget = fieldsRenderer(block.fields, Left(Stream.continually(block.fieldsWidth.toStream).flatten))

    override def afterSave(data:Json,form:JSONMetadata): Future[Json] = widget.afterSave(data,form)
    override def beforeSave(data:Json,form:JSONMetadata) = widget.beforeSave(data,form)

    override def killWidget(): Unit = widget.killWidget()

    override def field: JSONField = JSONField("block","block",false)

    override def afterRender() = widget.afterRender()

    override protected def show(): JsDom.all.Modifier = render(false)

    override protected def edit(): JsDom.all.Modifier = render(true)

    private def render(write:Boolean): JsDom.all.Modifier = div(BootstrapCol.md(12), ClientConf.style.subBlock)(
      block.title.map( t => h3(minHeight := 20.px, Labels(t))),  //renders title in subblocks
      widget.render(write,Property(true))
    )
  })

  private def simpleField(fieldName:String):WidgetVisibility = {for{
    field <- metadata.fields.find(_.name == fieldName)
  } yield {


    val fieldData = data.bitransform(_.js(field.name))((fd:Json) => data.get.deepMerge(Json.obj((field.name,fd))))

//    data.listen({ d =>
//      val newJs = d.js(field.name)
//      if( newJs != fieldData.get) {
//        fieldData.set(newJs)
//      }
//    },true)
//
//    fieldData.listen{ fd =>
//      if(data.get.js(field.name) != fd) {
//        data.set(data.get.deepMerge(Json.obj((field.name,fd))))
//      }
//    }

    WidgetVisibility(widgetSelector(field, id, fieldData),checkCondition(field))

  }}.getOrElse(WidgetVisibility(HiddenWidget.HiddenWidgetImpl(JSONField.empty)))


  private def fieldsRenderer(fields: Seq[Either[String, SubLayoutBlock]], horizontal: Either[Stream[Int],Boolean]):Widget = new Widget {

    val widgets:Seq[WidgetVisibility] = fields.map{
      case Left(fieldName) => simpleField(fieldName)
      case Right(subForm) => subBlock(subForm)
    }
    import io.circe.syntax._

    override def afterSave(value:Json,metadata:JSONMetadata): Future[Json] = saveAll(value,metadata,widgets.map(_.widget),_.afterSave)
    override def beforeSave(value:Json,metadata:JSONMetadata) = saveAll(value,metadata,widgets.map(_.widget),_.beforeSave)

    override def killWidget(): Unit = widgets.foreach(_.widget.killWidget())


    override def afterRender() = Future.sequence(widgets.map(_.widget.afterRender())).map(_.forall(x => x))

    override def field: JSONField = JSONField("fieldsRenderer","fieldsRenderer",false)

    override protected def show(): JsDom.all.Modifier = render(false)

    override protected def edit(): JsDom.all.Modifier = render(true)



    def fixedWidth(widths:Stream[Int],write:Boolean) : JsDom.all.Modifier = div(
      widgets.zip(widths).map { case (widget, width) =>
        div(BootstrapCol.md(width), ClientConf.style.field,
          widget.widget.render(write,widget.visibility)
        )
      }
    )

    def distribute(write:Boolean) : JsDom.all.Modifier = div(ClientConf.style.distributionContrainer,
      widgets.map { case widget =>
        div(ClientConf.style.field,
          widget.widget.render(write,widget.visibility)
        )
      }
    )

    private def render(write:Boolean): JsDom.all.Modifier = {
      horizontal match {
        case Left(widths) => fixedWidth(widths,write)
        case Right(_) => distribute(write)
      }
    }
  }



    val blocks: Seq[(LayoutBlock, Widget)] = metadata.layout.blocks.map { block =>
      val hLayout = block.distribute.contains(true) match {
        case true => Right(true)
        case false => Left(Stream.continually(12))
      }
      (
        block,
        fieldsRenderer(block.fields,hLayout)
      )
    }

    override def afterSave(value:Json, metadata:JSONMetadata): Future[Json] = saveAll(value,metadata,blocks.map(_._2),_.afterSave)
    override def beforeSave(value:Json, metadata:JSONMetadata) = saveAll(value,metadata,blocks.map(_._2),_.beforeSave)

  override def killWidget(): Unit = blocks.foreach(_._2.killWidget())


  override def afterRender() = Future.sequence(blocks.map(_._2.afterRender())).map(_.forall(x => x))

  override protected def show(): JsDom.all.Modifier = render(false)

  override def edit(): JsDom.all.Modifier = render(true)

  import io.udash._


  private def render(write:Boolean): JsDom.all.Modifier = {
    def renderer(block: LayoutBlock, widget:Widget) = {
      div(
        h3(block.title.map { title => Labels(title) }), //renders title in blocks
        widget.render(write, Property {
          true
        })
      ).render
    }

    val hasTabs:Boolean = blocks.flatMap(_._1.tab).distinct.nonEmpty

    def renderBlocks(b:Seq[(LayoutBlock,Widget)]) = b.map{ case (block,widget) =>
      div(BootstrapCol.md(block.width), ClientConf.style.block)(
        renderer(block,widget)
      )
    }

    div(
        div(ClientConf.style.jsonMetadataRendered,BootstrapStyles.Grid.row)(
          renderBlocks(blocks.filterNot(_._1.tabGroup.isDefined)),
          blocks.filter(_._1.tabGroup.isDefined).groupBy(_._1.tabGroup).toSeq.map{ case (_,blks) =>
            val tabs = blks.map(_._1.tab).distinct
            val selectedTab = Property(tabs.headOption.flatten)
            div(BootstrapCol.md(12),
              ul(BootstrapStyles.Navigation.nav, BootstrapStyles.Navigation.tabs, BootstrapStyles.Navigation.fill,
                tabs.map { name =>
                  val title = name.getOrElse("No title")

                  li(BootstrapStyles.Navigation.item,
                    a(BootstrapStyles.Navigation.link,
                      BootstrapStyles.active.styleIf(selectedTab.transform(_ == name)),
                      onclick :+= { (e: Event) => selectedTab.set(name); e.preventDefault() },
                      title
                    ).render
                  ).render
                }
              ),
              produce(selectedTab) { tabName =>
                renderBlocks(blks.filter(_._1.tab == tabName)).render
              }
            )
          }
        ),
        Debug(currentData,b => b, s"original data ${metadata.name}"),
        Debug(data,b => b, s"data ${metadata.name}")
    )
  }


}
