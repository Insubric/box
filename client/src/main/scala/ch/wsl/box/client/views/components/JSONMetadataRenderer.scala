package ch.wsl.box.client.views.components


import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.client.views.components
import ch.wsl.box.client.views.components.widget._
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
/**
  * Created by andre on 4/25/2017.
  */


case class JSONMetadataRenderer(metadata: JSONMetadata, data: Property[Json], children: Seq[JSONMetadata], id: Property[Option[String]]) extends ChildWidget  {


  import ch.wsl.box.client.Context._
  import io.circe._

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._


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

    val isKeyNotEditable:Boolean = {
      metadata.keys.contains(field.name) &&  //check if field is a key
        (
          id.get.isDefined ||                //if it's an existing record the key cannot be changed, it would be a new record
          (
            metadata.keyStrategy == SurrugateKey &&
            !( ClientConf.manualEditKeyFields || ClientConf.manualEditSingleKeyFields.contains(metadata.entity + "." + field.name))
          )
        )
    }

    val _field:JSONField = isKeyNotEditable match {
      case true => field.copy(readOnly = true)
      case false => field
    }

    val widg = field.widget match {
      case Some(value) => WidgetRegistry.forName(value)
      case None => WidgetRegistry.forType(field.`type`)
    }

    logger.debug(s"Selected widget for ${field.name}: ${widg}")

    widg.create(WidgetParams(id,fieldData,_field,metadata,data,children))

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

    override def afterRender(): Unit = widget.afterRender()

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


    override def afterRender(): Unit = widgets.foreach(_.widget.afterRender())

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



    val blocks = metadata.layout.blocks.map { block =>
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


  override def afterRender(): Unit = blocks.foreach(_._2.afterRender())

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

    div(
        Debug(data,autoRelease, "data"),
        div(ClientConf.style.jsonMetadataRendered,BootstrapStyles.Grid.row)(
          blocks.map{ case (block,widget) =>
            div(BootstrapCol.md(block.width), ClientConf.style.block)(
              renderer(block,widget)
            )
          }
        )
    )
  }


}
