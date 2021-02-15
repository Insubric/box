package ch.wsl.box.client.views.components.widget.child

import java.util.UUID

import ch.wsl.box.client.Context._
import ch.wsl.box.client.services.{ ClientSession, Labels}
import ch.wsl.box.client.views.components.JSONMetadataRenderer
import ch.wsl.box.client.views.components.widget.{ChildWidget, ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared._
import io.circe.Json
import io.udash._
import io.udash.properties.single.Property
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.Future

/**
  * Created by andre on 6/1/2017.
  */

case class ChildRow(widget:ChildWidget,id:String, data:ReadableProperty[Json], open:Boolean, rowId:Option[JSONID], deleted:Boolean=false)

trait ChildRendererFactory extends ComponentWidgetFactory {


  trait ChildRenderer extends Widget with Logging {

    def child:Child
    def children:Seq[JSONMetadata]
    def masterData:Property[Json]

    import ch.wsl.box.client.Context._
    import ch.wsl.box.shared.utils.JSONUtils._
    import io.circe._
    import io.circe.syntax._

    def row_id: Property[Option[String]]
    def prop: Property[Json]
    def field:JSONField

    val min:Int = field.params.flatMap(_.js("min").as[Int].toOption).getOrElse(0)
    val max:Option[Int] = field.params.flatMap(_.js("max").as[Int].toOption)

    val childWidgets: scala.collection.mutable.ListBuffer[ChildRow] = scala.collection.mutable.ListBuffer()
    val entity: SeqProperty[String] = SeqProperty(Seq())
    val metadata = children.find(_.objId == child.objId)

    protected def render(write: Boolean): JsDom.all.Modifier

    private def add(data:Json,open:Boolean): Unit = {

      val id = UUID.randomUUID().toString
      val propData = Property(data)
      val childId = Property(data.ID(metadata.get.keys).map(_.asString))

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

      val childRow = ChildRow(widget,id,propData,open,JSONID.fromData(propData.get,metadata.get))
      childWidgets += childRow
      entity.append(id)
      logger.debug(s"Added row ${childRow.rowId.map(_.asString).getOrElse("No ID")} of childForm ${metadata.get.name}")
      widget.afterRender()
    }

    def splitJson(js: Json): Seq[Json] = {
      js.as[Seq[Json]].right.getOrElse(Seq()) //.map{ js => js.deepMerge(Json.obj((subformInjectedId,Random.alphanumeric.take(16).mkString.asJson)))}
    }


    def removeItem(itemToRemove: String) = {
      logger.info("removing item")
      if (org.scalajs.dom.window.confirm(Labels.messages.confirm)) {
        entity.remove(itemToRemove)
        val childToDelete = childWidgets.zipWithIndex.find(x => x._1.id == itemToRemove).get
        childWidgets.update(childToDelete._2, childToDelete._1.copy(deleted = true))
      }
    }


    def addItem(child: Child, metadata: JSONMetadata) = {
      logger.info("adding item")


      val keys = for {
        (local, sub) <- child.masterFields.split(",").map(_.trim).zip(child.childFields.split(",").map(_.trim))
      } yield {
        //      println(s"local:$local sub:$sub")
        sub -> masterData.get.js(local)
      }
      keys.toMap
      val placeholder: Map[String, Json] = JSONMetadata.jsonPlaceholder(metadata, children) ++ keys.toMap

      //    println(placeholder)


      add(placeholder.asJson,true)
    }




    private def propagate[T](data: Json,f: (Widget => ((Json, JSONMetadata) => Future[T]))): Future[Seq[T]] = {

      val childMetadata = children.find(_.objId == child.objId).get

      val rows = data.seq(child.key)


      val out = Future.sequence(childWidgets.zipWithIndex.filterNot(x => x._1.deleted).map { case (cw,i) =>

        val oldData = cw.widget.data.get
        val newData = rows.lift(i).getOrElse(Json.obj())

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


      propagate(data, _.afterSave).map(collectData)
    }

    override def beforeSave(data: Json, metadata: JSONMetadata) = {
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
            metadata.map(_.objId).getOrElse(-1),
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




  }


}