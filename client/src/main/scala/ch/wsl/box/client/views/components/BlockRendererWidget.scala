package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.WidgetUtils.LabelLeft
import ch.wsl.box.client.views.components.widget.{HasData, HiddenWidget, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{ConditionalField, DistributedLayout, Internationalization, JSONField, JSONMetadata, LayoutType, MultirowTableLayout, StackedLayout, SubLayoutBlock, TableLayout}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssStyleName
import io.udash._
import org.scalajs.dom.Node
import scalatags.JsDom
import scalatags.JsDom.all.{div, h3, minHeight, s}

import scala.concurrent.Future



class BlockRendererWidget(widgetParams: WidgetParams, fields: Seq[Either[String, SubLayoutBlock]], layoutType:LayoutType, widths: Option[Stream[Int]] = None, titleSub:Option[String] = None, margin: Boolean = true) extends Widget with HasData {

  private case class WidgetVisibility(widget: Widget, visibility: ReadableProperty[Boolean])

  private object WidgetVisibility {
    def apply(widget: Widget): WidgetVisibility = WidgetVisibility(widget, Property(true))
  }


  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._
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
          val r = ConditionalField.check(value,condition.conditionValues)
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

  private val widgets:Seq[WidgetVisibility] = fields.flatMap{
    case Left(fieldName) => Seq(simpleField(fieldName))
    case Right(subForm) if layoutType == MultirowTableLayout => subForm.fields.flatMap {
      case Left(value) => Some(simpleField(value))
      case Right(value) => None
    }
    case Right(subForm) => Seq(subBlock(subForm))
  }
  import io.circe.syntax._

  private def subBlock(block: SubLayoutBlock):WidgetVisibility = WidgetVisibility(
    new BlockRendererWidget(widgetParams,block.fields, layoutType, Some(Stream.continually(block.fieldsWidth.getOrElse(Seq(12)).toStream).flatten),block.title.flatMap(Internationalization.either(services.clientSession.lang())).orElse(Some("")),false)
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

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderBlock(false,nested)

  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderBlock(true,nested)


  private def renderIfVisible(
                               widget:WidgetVisibility,
                               write:Boolean,
                               nested:Binding.NestedInterceptor)(
                                 renderer:(WidgetVisibility,Boolean) => Seq[Node]
  ) = {
    nested(showIf(widget.visibility) {
      widget.widget.load()
      renderer(widget,write)
    })
  }


  def fixedWidth(widths:Stream[Int],write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = {

    val setMargin:Modifier = if(!margin) ClientConf.style.removeFieldAndBlockMargin else Seq[Modifier]()

    div(BootstrapStyles.Grid.row,ClientConf.style.innerBlock,setMargin,
      widgets.zip(widths).zipWithIndex.map { case ((widget, width),i) =>


        div(CssStyleName(s"block-el-$i"),BootstrapCol.md(width), ClientConf.style.field,if(!widget.widget.subForm) ClientConf.style.fieldHighlight else Seq[Modifier](),
          renderIfVisible(widget,write,nested)( (widget,write) =>
            div(widget.widget.render(write,nested)).render
          )
        )
      }
    )
  }

  def distribute(write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = div(ClientConf.style.distributionContrainer,
    widgets.map { case widget =>
      div(ClientConf.style.field,if(!widget.widget.subForm) ClientConf.style.fieldHighlight else Seq[Modifier](),
        renderIfVisible(widget,write,nested)( (widget,write) =>
          div(widget.widget.render(write,nested)).render
        )
      )
    }
  )

  def tableRenderer(write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = table(ClientConf.style.table,
    tr(
      widgets.map { case widget =>
        renderIfVisible(widget,write,nested)( (widget,write) =>
            th(ClientConf.style.field,WidgetUtils.toLabel(widget.widget.field,LabelLeft)).render
        )
      }
    ),
    tr(
      widgets.map { case widget =>
        renderIfVisible(widget,write,nested)( (widget,write) =>
          td(if(!widget.widget.subForm) ClientConf.style.fieldHighlight else Seq[Modifier](),
              widget.widget.renderOnTable(write,nested)
          ).render
        )
      }
    )
  )

  def multiLineRableRenderer(write:Boolean,nested:Binding.NestedInterceptor) : JsDom.all.Modifier = {

    val blocks = fields.flatMap(_.toOption)
    val rowCount:Int = blocks.headOption.map(_.fields.count(_.isLeft)).getOrElse(0)



    val temp: Seq[Seq[Option[WidgetVisibility]]] = (0 until rowCount).map { j =>
      (0 until blocks.size).map { i =>
        widgets.lift(j + i*rowCount)
      }
    }

    div(ClientConf.style.tableContainer,
      table(ClientConf.style.table,
        tr(
            blocks.map{ b =>
              th(ClientConf.style.field,b.title.flatMap(Internationalization.either(services.clientSession.lang()))).render
            }
        ),

          temp.map { wdgts =>
          tr(
            wdgts.map{ widget =>
              val r:Modifier = widget match {
                case Some(value) => renderIfVisible(value, write, nested)((widget, write) =>
                  td(if (!widget.widget.subForm) ClientConf.style.fieldHighlight else Seq[Modifier](),
                    widget.widget.renderOnTable(write, nested)
                  ).render
                )
                case None => Seq[Modifier]()
              }
              r
            }
          ).render
        }.toSeq

      )
    )
  }


  private def renderBlock(write:Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    logger.info(s"blockname: $titleSub type: $layoutType")

    def ren() = layoutType match {
      case StackedLayout => fixedWidth(widths.getOrElse(Stream.continually(12)), write,nested)
      case DistributedLayout => distribute(write,nested)
      case TableLayout => tableRenderer(write,nested)
      case MultirowTableLayout => multiLineRableRenderer(write,nested)
    }




    div(BootstrapCol.md(12), ClientConf.style.subBlock)(
      if(titleSub.exists(_ != "")) h3(minHeight := 20.px, Labels(titleSub.get)) else {},  //renders title in subblocks
      ren()
    )


  }
}