package ch.wsl.box.client.views.components.widget.child

import java.util.UUID
import ch.wsl.box.client.Context._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.JSONMetadataRenderer
import ch.wsl.box.client.views.components.widget.{ChildWidget, ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared._
import io.circe.Json
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.properties.single.Property
import org.scalajs.dom.Event
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.Future

/**
  * Created by andre on 6/1/2017.
  */

case class ChildRow(widget:ChildWidget,id:String, data:Property[Json], open:Property[Boolean],metadata:Option[JSONMetadata], deleted:Boolean=false) {
  def rowId:ReadableProperty[Option[JSONID]] = data.transform(js => metadata.flatMap(m => JSONID.fromData(js,m)))
  def rowIdStr:ReadableProperty[String] = rowId.transform(_.map(_.asString).getOrElse("noid"))
}

trait ChildRendererFactory extends ComponentWidgetFactory {


  trait ChildRenderer extends Widget with Logging {

    import io.udash.css.CssView._
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._


    def widgetParam:WidgetParams



    def child:Child
    def children:Seq[JSONMetadata] = widgetParam.children
    def masterData:ReadableProperty[Json] = widgetParam.allData

    import ch.wsl.box.client.Context._
    import ch.wsl.box.shared.utils.JSONUtils._
    import io.circe._
    import io.circe.syntax._

    def row_id: ReadableProperty[Option[String]] = widgetParam.id
    def prop: Property[Json] = widgetParam.prop
    def field:JSONField = widgetParam.field

    val min:Int = field.params.flatMap(_.js("min").as[Int].toOption).getOrElse(0)
    val max:Option[Int] = field.params.flatMap(_.js("max").as[Int].toOption)

    val disableAdd = field.params.exists(_.js("disableAdd") == true.asJson)
    val disableRemove = field.params.exists(_.js("disableRemove") == true.asJson)

    val childWidgets: scala.collection.mutable.ListBuffer[ChildRow] = scala.collection.mutable.ListBuffer()
    val entity: SeqProperty[String] = SeqProperty(Seq())
    val metadata = children.find(_.objId == child.objId)

    val changedField = widgetParam.otherField("$changed")

    protected def render(write: Boolean): JsDom.all.Modifier

    private def add(data:Json,open:Boolean): Unit = {

      val props:ReadableProperty[Json] = masterData.transform{js =>
        child.props.map(p => p -> js.js(p)).toMap.asJson
      }

      val id = UUID.randomUUID().toString
      val propData = Property(data.deepMerge(props.get))
      val childId = Property(data.ID(metadata.get.keys).map(_.asString))

      props.listen(p => propData.set(propData.get.deepMerge(p)))

      propData.listen{data =>
        val newData = prop.get.as[Seq[Json]].toSeq.flatten.map{x =>
          if(x.ID(metadata.get.keys) == data.ID(metadata.get.keys)) {
            x.deepMerge(data)
          } else x
        }
        propListener.cancel()
        prop.set(newData.asJson)
        registerListener(false)
      }

      val widget = JSONMetadataRenderer(metadata.get, propData, children, childId)

      val childRow = ChildRow(widget,id,propData,Property(open),metadata)
      childWidgets += childRow
      entity.append(id)
      logger.debug(s"Added row ${childRow.rowId.get.map(_.asString).getOrElse("No ID")} of childForm ${metadata.get.name}")
      widget.afterRender()
    }

    def splitJson(js: Json): Seq[Json] = {
      js.as[Seq[Json]].right.getOrElse(Seq()) //.map{ js => js.deepMerge(Json.obj((subformInjectedId,Random.alphanumeric.take(16).mkString.asJson)))}
    }


    def removeItem(itemToRemove: => ChildRow) = (e:Event) => {
      logger.info("removing item")
      if (org.scalajs.dom.window.confirm(Labels.messages.confirm)) {
        val childToDelete = childWidgets.zipWithIndex.find(x => x._1.rowId.get == itemToRemove.rowId.get && x._1.id == itemToRemove.id).get
        entity.remove(childToDelete._1.id)
        childWidgets.update(childToDelete._2, childToDelete._1.copy(deleted = true))
        changedField.set(true.asJson)
      }
    }

    def addItemHandler(child: => Child, metadata: => JSONMetadata) = (e:Event) => {
      addItem(child,metadata)
      e.preventDefault()
    }
    def addItem(child: Child, metadata: JSONMetadata) =  {
      logger.info("adding item")


      val keys = for {
        m <- child.mapping
      } yield {
        //      println(s"local:$local sub:$sub")
        m.child -> masterData.get.js(m.parent)
      }

      val placeholder: Map[String, Json] = JSONMetadata.jsonPlaceholder(metadata, children) ++ keys.toMap

      //    println(placeholder)


      add(placeholder.asJson,true)
    }




    private def propagate[T](data: Json,f: (Widget => ((Json, JSONMetadata) => Future[T]))): Future[Seq[T]] = {

      val childMetadata = children.find(_.objId == child.objId).get

      val rows = data.seq(child.key)
      BrowserConsole.log(io.circe.scalajs.convertJsonToJs(rows.asJson))

      val out = Future.sequence(childWidgets.filterNot(_.deleted).map{ case cw =>


        val oldData = cw.data.get
        val newData = rows.find(r => metadata.exists(m => JSONID.fromData(r,m) == cw.rowId.get )).getOrElse(Json.obj())

        logger.debug(s"olddata: $oldData")
        logger.debug(s"newdata: $newData")

        val d = oldData.deepMerge(newData)

        logger.debug(s"result: $newData")

        f(cw.widget)(d, childMetadata).map{ r =>
          r
        }
      }.toSeq)
      //correct futures
      out
    }

    def collectData(jsChilds:Seq[Json]) = {
      logger.debug(Map(child.key -> jsChilds.asJson).asJson.toString())
      Map(child.key -> jsChilds.asJson).asJson
    }

    override def afterSave(data: Json, m: JSONMetadata): Future[Json] = {
      logger.info(data.toString())
      metadata.foreach { met =>
        //Set new inserted records open by default
        val oldData: Seq[JSONID] = this.prop.get.as[Seq[Json]].getOrElse(Seq()).flatMap(x => JSONID.fromData(x, met))
        val newData: Seq[JSONID] = data.seq(field.name).flatMap(x => JSONID.fromData(x, met))

        newData.foreach{ id =>
          if(!oldData.contains(id)) {
            services.clientSession.setTableChildOpen(ClientSession.TableChildElement(field.name,met.objId,Some(id)))
          }
        }

      }

      logger.debug("After save")
      propagate(data, _.afterSave).map(collectData)
    }

    override def beforeSave(data: Json, metadata: JSONMetadata) = {
      logger.debug("Before save")
      propagate(data, _.beforeSave).map(collectData)
    }

    override def killWidget(): Unit = {
      super.killWidget()
      childWidgets.foreach(_.widget.killWidget())
    }


    override def afterRender(): Unit = childWidgets.foreach(_.widget.afterRender())


    override protected def show(): JsDom.all.Modifier = render(false)

    override protected def edit(): JsDom.all.Modifier = render(true)



    var propListener:Registration = null

    def registerListener(immediate:Boolean) {
      propListener = prop.listen(i => {
        childWidgets.foreach(_.widget.killWidget())
        childWidgets.clear()
        entity.clear()
        val entityData = splitJson(prop.get)


        entityData.foreach { x =>
          val isOpen:Boolean = services.clientSession.isTableChildOpen(ClientSession.TableChildElement(
            field.name,
            metadata.map(_.objId).getOrElse(UUID.randomUUID()),
            metadata.flatMap(m => JSONID.fromData(x,m))
          ))
          add(x, isOpen)
        }

        for(i <- 0 until (min - entityData.length)) yield {
          logger.info(i.toString)
          metadata.map { m =>
            addItem(child, m)
          }
        }
      }, immediate)
    }

    registerListener(true)


    def addButton(write:Boolean,m:JSONMetadata) = {
      val name = widgetParam.field.label.getOrElse(widgetParam.field.name)
      if (write && !disableAdd) {
        autoRelease(showIf(entity.transform(e => max.forall(_ > e.length))) {
          a(id := TestHooks.addChildId(m.objId),
            ClientConf.style.childAddButton,
            onclick :+= addItemHandler(child,m),
            Icons.plusFill, name
          ).render
        })
      } else frag()
    }

    def removeButton(write:Boolean,widget: ChildRow,m:JSONMetadata) = {
      val border = widgetParam.field.params.exists(_.js("noBorder") == Json.True) match {
        case false => Seq(ClientConf.style.block,ClientConf.style.withBorder)
        case true => Seq(ClientConf.style.block)
      }
      val name = widgetParam.field.label.getOrElse(widgetParam.field.name)
      if (write && !disableRemove) {
        autoRelease(showIf(entity.transform(_.length > min)) {
          div(
            BootstrapStyles.Grid.row,
            div(BootstrapCol.md(12), border,
              div(BootstrapStyles.Float.right(),
                a(ClientConf.style.childRemoveButton,
                  onclick :+= removeItem(widget),
                  Icons.minusFill, name,
                  id.bind(widget.rowId.transform(x => TestHooks.deleteChildId(m.objId,x))))
              )
            )
          ).render
        })
      } else frag()
    }


  }




}