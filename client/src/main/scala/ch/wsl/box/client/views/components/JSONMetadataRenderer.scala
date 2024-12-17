package ch.wsl.box.client.views.components


import ch.wsl.box.client.services.ClientSession.SelectedTabKey
import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components
import ch.wsl.box.client.views.components.widget._
import ch.wsl.box.client.views.components.widget.child.ChildRenderer
import ch.wsl.box.client.views.components.widget.labels.{StaticTextWidget, TitleWidget}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import io.circe.syntax.EncoderOps
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


case class JSONMetadataRenderer(metadata: JSONMetadata, data: Property[Json], children: Seq[JSONMetadata], id: Property[Option[String]],actions: WidgetCallbackActions,changed:Property[Boolean], public:Boolean) extends ChildWidget  {


  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._
  import io.circe._

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  private val currentData:Property[Json] = Property(data.get)

  def resetChanges() = {
    logger.info(s"${metadata.name} resetChanges")
    blocks.foreach(_.widget.resetChangeAlert())
    currentData.set(data.get)
  }

  data.listen { d =>
    logger.debug(s"changed ${d.js(ChildRenderer.CHANGED_KEY)} on ${metadata.name}")
    if(d.js(ChildRenderer.CHANGED_KEY) == Json.True) {
      changed.set(true,true)
      logger.debug(s"${metadata.name} has changes in child")
    } else if(!currentData.get.removeNonDataFields(metadata,children,false).equals(d.removeNonDataFields(metadata,children,false))) {
      changed.set(true,true)
      logger.debug(s"""
                ${metadata.name} has changes

                original:
                ${currentData.get.removeNonDataFields(metadata,children,false)}

                new:
                ${d.removeNonDataFields(metadata,children,false)}

                """)

    } else {
      changed.set(false,true)
    }
  }

  override def field: JSONField = JSONField("metadataRenderer","metadataRenderer",false)

  private def getId(data:Json): Option[String] = {
    if(metadata.static) id.get
    else
      data.ID(metadata.keyFields).map(_.asString)
  }

  data.listen { data =>
    val currentID = getId(data)
    if (currentID != id.get) {
      id.set(currentID)
    }
  }



  private def saveAll(data:Json,widgetAction:Widget => (Json,JSONMetadata) => Future[Json]):Future[Json] = {
    // start to empty and populate



    val blocksResult:Future[Seq[Json]] = Future.sequence(blocks.map{b =>
      widgetAction(b.widget)(data,metadata).map { intermediateResult =>
        val fields:Seq[String] = b.layoutBlock.toList.flatMap(_.extractFields(metadata))
        logger.debug(s"metadata: ${metadata.name} intermediateResult: $intermediateResult \n\n ${b.fields}")
        Json.fromFields(fields.flatMap{k =>
          intermediateResult.jsOpt(k).map(v => k -> v)
        })
      }
    })



    blocksResult.map{ js =>
      logger.debug(s"metadata: ${metadata.name} blocksResult: $js \n\n data: $data")
      val defaults:Seq[(String,Json)] = metadata.fields.filter(_.default.isDefined).flatMap{f =>
        data.jsOpt(f.name)
            .orElse(JSONUtils.toJs(f.default.get,f.`type`))
            .map(v => f.name -> v)
      }

      val keys = metadata.keys.map(k => k -> data.js(k))

      val base:Json = Json.fromFields(defaults ++ keys)
      val result = js.foldLeft(base){ (acc,n) => acc.deepMerge(n)}
      logger.debug(s"metadata: ${metadata.name} merge: $result")
      result
    }



  }

  case class FormBlock(widget:Widget,fields:Seq[String],layoutBlock: Option[LayoutBlock])

  val blocks: Seq[FormBlock] = metadata.layout.blocks.map { block =>

    FormBlock(
      new BlockRendererWidget(WidgetParams(id,data,field,metadata,data,children,actions,public),block.fields,block.layoutType),
      block.extractFields(metadata),
      Some(block),
    )
  }


  override def beforeSave(value:Json, metadata:JSONMetadata) = saveAll(value,_.beforeSave)

  override def killWidget(): Unit = blocks.foreach(_.widget.killWidget())


  override def afterRender() = Future.sequence(blocks.map(_.widget.afterRender())).map(_.forall(x => x))

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderJsonMetadata(false,nested)

  override def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderJsonMetadata(true,nested)

  import io.udash._

  case class WidgetBlock(block:LayoutBlock,widget:Widget)
  object WidgetBlock{
    def ofTab(block:LayoutBlock,widget:Widget) = WidgetBlock(block.copy(width = 12), widget)
  }

  private def renderJsonMetadata(write:Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    def renderBlocks(b:Seq[WidgetBlock]) = b.map{ case WidgetBlock(block,widget) =>
      div(BootstrapCol.md(block.width), ClientConf.style.block)(
        div(
          block.title.flatMap(Internationalization.either(services.clientSession.lang())) match {
            case Some(value) => h3(Labels(value))
            case None => frag()
          }, //renders title in blocks
          widget.render(write, nested)
        )
      )
    }

    div(
        div(ClientConf.style.jsonMetadataRendered,BootstrapStyles.Grid.row)(
          renderBlocks(blocks.filter(_.layoutBlock.isDefined).filterNot(_.layoutBlock.flatMap(_.tabGroup).isDefined).map(x => WidgetBlock(x.layoutBlock.get,x.widget))),
          blocks.filter(_.layoutBlock.flatMap(_.tabGroup).isDefined).groupBy(_.layoutBlock.map(_.tabGroup).get).toSeq.map{ case (tabGroup,blks) =>
            val tabs = blks.map(_.layoutBlock.get.tab).distinct
            val tabKey = SelectedTabKey(metadata.objId,tabGroup)
            val selectedTab = Property(services.clientSession.selectedTab(tabKey).orElse(tabs.headOption.flatten))
            div(BootstrapCol.md(blks.map(_.layoutBlock.get.width).max),
              ul(BootstrapStyles.Navigation.nav, BootstrapStyles.Navigation.tabs, BootstrapStyles.Navigation.fill,
                tabs.map { tabId =>
                  val title:String = blocks.find(_.layoutBlock.exists(_.tab == tabId))
                    .flatMap(_.layoutBlock.flatMap(_.title).flatMap(Internationalization.either(services.clientSession.lang()))).orElse(tabId).getOrElse("")

                  li(BootstrapStyles.Navigation.item,
                    a(BootstrapStyles.Navigation.link,
                      BootstrapStyles.active.styleIf(selectedTab.transform(_ == tabId)),
                      onclick :+= { (e: Event) =>
                        selectedTab.set(tabId);
                        tabId.foreach(n => services.clientSession.setSelectedTab(tabKey,n))
                        e.preventDefault() },
                      title
                    ).render
                  ).render
                }
              ),
              nested(produce(selectedTab) { tabName =>
                renderBlocks(blks.filter(_.layoutBlock.get.tab == tabName).map(x => WidgetBlock.ofTab(x.layoutBlock.get,x.widget))).render
              })
            )
          }
        ),
        Debug(currentData,b => b, s"original data ${metadata.name}"),
        Debug(data,b => b, s"data ${metadata.name}")
    )
  }


}
