package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{HasData, HiddenWidget, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, SubLayoutBlock}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.{Property, ReadableProperty}
import scalatags.JsDom
import scalatags.JsDom.all.{div, h3, minHeight, s}

import scala.concurrent.Future



class BlockRendererWidget(widgetParams: WidgetParams,fields: Seq[Either[String, SubLayoutBlock]], horizontal: Either[Stream[Int],Boolean], titleSub:Option[String] = None, margin: Boolean = true) extends Widget with HasData {

  private case class WidgetVisibility(widget: Widget, visibility: ReadableProperty[Boolean])

  private object WidgetVisibility {
    def apply(widget: Widget): WidgetVisibility = WidgetVisibility(widget, Property(true))
  }


  import ch.wsl.box.client.Context._
  import io.circe._

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  def data = widgetParams.prop



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
          val r = condition.check(value)
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

  private def widgetSelector(field: JSONField, fieldData:Property[Json]): Widget = {



    val _field:JSONField = WidgetUtils.isKeyNotEditable(widgetParams.metadata,field,widgetParams.id.get) match {
      case true => field.copy(readOnly = true)
      case false => field
    }

    val widg = field.widget match {
      case Some(value) => WidgetRegistry.forName(value)
      case None => WidgetRegistry.forType(field.`type`)
    }

    logger.debug(s"Selected widget for ${field.name}: ${widg}")

    widg.create(WidgetParams(widgetParams.id,fieldData,_field,widgetParams.metadata,data,widgetParams.children,widgetParams.actions,widgetParams.public))

  }



  private def simpleField(fieldName:String):WidgetVisibility = {for{
    field <- widgetParams.metadata.fields.find(_.name == fieldName)
  } yield {

    val fieldData = data.bitransform(_.js(field.name))((fd:Json) => data.get.deepMerge(Json.obj((field.name,fd))))

    WidgetVisibility(widgetSelector(field, fieldData),checkCondition(field))

  }}.getOrElse(WidgetVisibility(HiddenWidget.HiddenWidgetImpl(JSONField.empty)))

  private val widgets:Seq[WidgetVisibility] = fields.map{
    case Left(fieldName) => simpleField(fieldName)
    case Right(subForm) => subBlock(subForm)
  }
  import io.circe.syntax._

  private def subBlock(block: SubLayoutBlock):WidgetVisibility = WidgetVisibility(
    new BlockRendererWidget(widgetParams,block.fields, Left(Stream.continually(block.fieldsWidth.toStream).flatten),block.title.orElse(Some("")),false)
  )


  private def saveAll(data:Json, widgets:Seq[Widget],widgetAction:Widget => (Json,JSONMetadata) => Future[Json]):Future[Json] = {
    // start to empty and populate
    logger.debug(
      s"""
         |BlockRenderer of ${field.name}
         |with widgets: ${widgets.map(_.field.name)}
         |""".stripMargin)
    widgets.foldLeft(Future.successful(Json.Null)){ (result,widget) =>
      for{
        r <- result
        // always pass the full data to the sub widget
        newResult <- widgetAction(widget)(data,widgetParams.metadata)
      } yield {

        val k = widget.field.name

        val result = widget match {
          case blockRendererWidget: BlockRendererWidget => {
            val newObj = newResult.asObject.map(_.asJson).getOrElse(Json.obj()) // handle the case with empty subblock
            r.deepMerge(newObj)
          }
          case _ => r.deepMerge(Map(k -> newResult.jsOrDefault(widget.field)).asJson)
        }

        logger.debug(
          s"""
             |block field: ${widget.field.name}
             |widget: $widget
             |original:
             |$r
             |
             |new:
             |${newResult.removeEmptyArray}
             |
             |merged:
             |$result
             |""".stripMargin)
        result
      }
    }
  }


  override def beforeSave(value:Json,metadata:JSONMetadata) = {
    // WSS-228 when a field is hidden ignore it for persistence
    logger.info(s"metadata: ${metadata.name} All: ${widgets.map(_.widget.field.name)}, visible or default only: ${widgets.filter(x => x.visibility.get || x.widget.field.default.isDefined).map(_.widget.field.name)}")
    saveAll(value,widgets.filter(x => x.visibility.get || x.widget.field.default.isDefined).map(_.widget),_.beforeSave)
  }

  override def killWidget(): Unit = widgets.foreach(_.widget.killWidget())


  override def afterRender() = Future.sequence(widgets.map(_.widget.afterRender())).map(_.forall(x => x))

  override def field: JSONField = JSONField("fieldsRenderer","fieldsRenderer",false)

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = render(false,nested)

  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = render(true,nested)



  def fixedWidth(widths:Stream[Int],write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = {

    val setMargin:Modifier = if(!margin) ClientConf.style.removeFieldAndBlockMargin else Seq[Modifier]()

    div(BootstrapStyles.Grid.row,ClientConf.style.innerBlock,setMargin,
      widgets.zip(widths).map { case (widget, width) =>


        div(BootstrapCol.md(width), ClientConf.style.field,
          widget.widget.render(write,widget.visibility,nested)
        )
      }
    )
  }

  def distribute(write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = div(ClientConf.style.distributionContrainer,
    widgets.map { case widget =>
      div(ClientConf.style.field,
        widget.widget.render(write,widget.visibility,nested)
      )
    }
  )


  private def render(write:Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    logger.info(s"blockname: $titleSub horizontal: $horizontal")

    def ren() = horizontal match {
      case Left(widths) => fixedWidth(widths, write,nested)
      case Right(_) => distribute(write,nested)
    }


    div(BootstrapCol.md(12), ClientConf.style.subBlock)(
      if(titleSub.exists(_ != "")) h3(minHeight := 20.px, Labels(titleSub.get)) else {},  //renders title in subblocks
      ren()
    )


  }
}