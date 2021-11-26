package ch.wsl.box.client.views.components


import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
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
    logger.debug(s"changed ${d.js(ChildRenderer.CHANGED_KEY)} on ${metadata.name}")
    if(d.js(ChildRenderer.CHANGED_KEY) == Json.True) {
      changed.set(true,true)
      logger.debug(s"${metadata.name} has changes in child")
    } else if(!currentData.get.removeNonDataFields.equals(d.removeNonDataFields)) {
      changed.set(true,true)
      logger.debug(s"""
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



  private def saveAll(data:Json,widgetAction:Widget => (Json,JSONMetadata) => Future[Json]):Future[Json] = {
    // start to empty and populate

    def extractFields(fields:Either[String,SubLayoutBlock]):Seq[String] = fields match {
      case Left(value) if metadata.fields.exists(_.name == value) => Seq(value)
      case Left(_) => Seq()
      case Right(value) => value.fields.flatMap(extractFields)
    }

    val blocksResult:Future[Seq[Json]] = Future.sequence(blocks.map{b =>
      widgetAction(b._2)(data,metadata).map { intermediateResult =>
        val fields:Seq[String] = b._1.fields.flatMap(extractFields)
        logger.debug(s"metadata: ${metadata.name} intermediateResult: $intermediateResult \n\n $fields")
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

  val blocks: Seq[(LayoutBlock, Widget)] = metadata.layout.blocks.map { block =>
    val hLayout = block.distribute.contains(true) match {
      case true => Right(true)
      case false => Left(Stream.continually(12))
    }
    (
      block,
      new BlockRendererWidget(WidgetParams(id,data,field,metadata,data,children,actions,public),block.fields,hLayout)
    )
  }

  override def afterSave(value:Json, metadata:JSONMetadata): Future[Json] = {
    logger.debug(s"JSONMetadataRenderer after save for ${metadata.name} with $value")
    saveAll(value,_.afterSave)
  }
  override def beforeSave(value:Json, metadata:JSONMetadata) = saveAll(value,_.beforeSave)

  override def killWidget(): Unit = blocks.foreach(_._2.killWidget())


  override def afterRender() = Future.sequence(blocks.map(_._2.afterRender())).map(_.forall(x => x))

  override protected def show(): JsDom.all.Modifier = render(false)

  override def edit(): JsDom.all.Modifier = render(true)

  import io.udash._


  private def render(write:Boolean): JsDom.all.Modifier = {

    def renderBlocks(b:Seq[(LayoutBlock,Widget)]) = b.map{ case (block,widget) =>
      div(BootstrapCol.md(block.width), ClientConf.style.block)(
        div(
          if(block.title.exists(_.nonEmpty)) {
            h3(block.title.map { title => Labels(title) })
          } else frag(), //renders title in blocks
          widget.render(write, Property(true))
        )
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
