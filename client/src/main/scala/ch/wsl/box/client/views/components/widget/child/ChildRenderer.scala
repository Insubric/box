package ch.wsl.box.client.views.components.widget.child

import java.util.UUID
import ch.wsl.box.client.Context._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.JSONMetadataRenderer
import ch.wsl.box.client.views.components.widget.{ChildWidget, ComponentWidgetFactory, Widget, WidgetCallbackActions, WidgetParams}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.generic.auto._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.utils.UdashIcons
import io.udash.properties.single.Property
import org.scalajs.dom.{Event, HTMLAnchorElement, document, window}
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.Future
import scala.scalajs.js.timers.setTimeout

/**
  * Created by andre on 6/1/2017.
  */

case class ChildRow(widget:ChildWidget,id:String, data:Property[Json], open:Property[Boolean],metadata:Option[JSONMetadata], changed:Property[Boolean], changedListener:Registration, newRow:Boolean, deleted:Boolean=false) {
  def rowId:ReadableProperty[Option[JSONID]] = data.transform(js => metadata.flatMap(m => JSONID.fromData(js,m,false)))
  def rowIdStr:ReadableProperty[String] = rowId.transform(_.map(_.asString).getOrElse("noid"))
}

object ChildRenderer{
  val CHANGED_KEY = "$changed"
}

trait ChildRendererFactory extends ComponentWidgetFactory {


  trait ChildRenderer extends Widget with Logging {

    import io.udash.css.CssView._
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._


    def widgetParam:WidgetParams


    def child:Child = field.child match {
      case Some(value) => value
      case None => throw new Exception(s" ${field.name} does not have a child")
    }

    def children:Seq[JSONMetadata] = widgetParam.children
    def masterData:ReadableProperty[Json] = widgetParam.allData

    import ch.wsl.box.client.Context._
    import ch.wsl.box.shared.utils.JSONUtils._
    import io.circe._
    import io.circe.syntax._

    def row_id: ReadableProperty[Option[String]] = widgetParam.id
    def prop: Property[Json] = widgetParam.prop
    def field:JSONField = widgetParam.field

    val min:Int = Child.min(field)
    val max:Option[Int] = Child.max(field)

    val disableAdd = field.params.exists(_.js("disableAdd") == true.asJson)
    val disableRemove = field.params.exists(_.js("disableRemove") == true.asJson)
    val disableDuplicate = field.params.exists(_.js("disableDuplicate") == true.asJson)
    val duplicateIcon:Icons.Icon = {
      widgetParam.field.params.flatMap(_.getOpt("duplicateIcon")) match {
        case Some("add") => Icons.plusFill
        case _ => Icons.duplicate
      }
    }
    val enableDeleteOnlyNew = field.params.exists(_.js("enableDeleteOnlyNew") == true.asJson)
    val duplicateIgnoreFields:Seq[String] = field.params.toSeq.flatMap(_.js("duplicateIgnoreFields").as[Seq[String]].toOption).flatten
    val sortable = field.params.exists(_.js("sortable") == true.asJson)

    val childWidgets: scala.collection.mutable.ListBuffer[ChildRow] = scala.collection.mutable.ListBuffer()
    def getWidget(id:String):ChildRow = childWidgets.find(_.id == id) match {
      case Some(value) => value
      case None => throw new Exception(s"Widget not found $id")
    }
    val entity: SeqProperty[String] = SeqProperty(Seq())
    val metadata = children.find(_.objId == child.objId)

    val changedField = widgetParam.otherField(ChildRenderer.CHANGED_KEY)
    var countChilds = 0


    override def resetChangeAlert(): Unit = {
      countChilds = childWidgets.filterNot(_.deleted).length
      changedField.set(Json.False)
    }

    def checkChanges() = setTimeout(0) {
      if(countChilds != childWidgets.filterNot(_.deleted).length) {
        logger.info(s"Set $$changed on ${metadata.map(_.name).getOrElse("")} because of length $countChilds !=  ${childWidgets.filterNot(_.deleted).length}")
        changedField.set(Json.True)
      } else if(childWidgets.exists(_.changed.get)) {
        logger.info(s"Set $$changed on ${metadata.map(_.name).getOrElse("")} because of changes in ${childWidgets.filter(_.changed.get).map(x => x.metadata.map(_.name).getOrElse("No name"))}")
        changedField.set(Json.True)
      } else {
        logger.info(s"Set $$changed false on ${metadata.map(_.name).getOrElse("")}")
        changedField.set(Json.False)
      }
    }

    protected def render(write: Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier


    private def add(data:Json,open:Boolean,newRow:Boolean, place:Option[Int] = None): Unit = {

      val props:ReadableProperty[Json] = masterData.transform{js =>
        child.props.map(p => p -> js.js(p)).toMap.asJson
      }

      val id = UUID.randomUUID().toString
      val propData = Property(data.deepMerge(props.get))
      val childId = Property(data.ID(metadata.get.keyFields).map(_.asString))

      props.listen(p => propData.set(propData.get.deepMerge(p)))

      propData.listen{data =>
        val newData = prop.get.as[Seq[Json]].toSeq.flatten.map{x =>
          if(x.ID(metadata.get.keyFields).nonEmpty && x.ID(metadata.get.keyFields) == data.ID(metadata.get.keyFields)) {
            x.deepMerge(data)
          } else x
        }
        if(propListener != null)
          propListener.cancel()
        prop.set(newData.asJson)
        registerListener(false)
      }

      val changed = Property(false)
      val actions = widgetParam.actions.copy(save = () => widgetParam.actions.save().map{case (parentId,parentData) =>
        (parentId,parentData.seq(field.name).lift(entity.get.indexOf(id)).getOrElse(Json.Null))
      } )

      val widget = JSONMetadataRenderer(metadata.get, propData, children, childId,actions,changed,widgetParam.public)

      val changeListener = changed.listen(_ => checkChanges())

      val childRow = ChildRow(widget,id,propData,Property(open),metadata,changed,changeListener,newRow)
      place match {
        case Some(idx) => {
          childWidgets.insert(idx,childRow)
          entity.insert(idx,id)
        }
        case None => { //append at the end
          childWidgets += childRow
          entity.append(id)
        }
      }

      logger.debug(s"Added row ${childRow.rowId.get.map(_.asString).getOrElse("No ID")} of childForm ${metadata.get.name}")
      widget.afterRender()
    }

    def splitJson(js: Json): Seq[Json] = {
      js.as[Seq[Json]].right.getOrElse(Seq()) //.map{ js => js.deepMerge(Json.obj((subformInjectedId,Random.alphanumeric.take(16).mkString.asJson)))}
    }

    def findItem(item: => ChildRow) = childWidgets.zipWithIndex.find(x => x._1.rowId.get == item.rowId.get && x._1.id == item.id)

    def removeItem(itemToRemove: => ChildRow) = (e:Event) => {
      logger.info("removing item")
      if (org.scalajs.dom.window.confirm(Labels.messages.confirm)) {
        findItem(itemToRemove).map { case (row, idx) =>
          entity.remove(row.id)
          childWidgets.update(idx, row.copy(deleted = true))
          checkChanges()
        }
      }
    }

    def up(item: => ChildRow) = (e:Event) => {
      findItem(item).map { case (_, idx) =>
        swapItems(idx - 1, idx)
      }
    }

    def down(item: => ChildRow) = (e:Event) => {
      findItem(item).map { case (_, idx) =>
        swapItems(idx+1,idx)
      }
    }

    private def swapItems(a: Int,b: Int) =  {
      logger.info("swap item")
      val itemA = childWidgets(a)
      val itemB = childWidgets(b)

      childWidgets.mapInPlace{ i =>
        if(i == itemA) itemB
        else if(i == itemB) itemA
        else i
      }
      val et = entity.get.map{ i =>
        if(i == itemA.id) itemB.id
        else if(i == itemB.id) itemA.id
        else i
      }
      entity.set(et)
      logger.info(s"Set $$changed on ${metadata.map(_.name).getOrElse("")} because of position swap in ${childWidgets.filter(_.changed.get).map(x => x.metadata.map(_.name).getOrElse("No name"))}")
      changedField.set(Json.True)

    }

    def duplicateItem(itemToDuplicate: => ChildRow) = (e:Event) => {
      itemToDuplicate.metadata match {
        case Some(md) => {
          def dataWithoutIgnored = itemToDuplicate.data.get.mapObject(obj => JsonObject.fromMap(obj.toMap.filterNot { case (key, _) => duplicateIgnoreFields.contains(key) }))
          def dataWithNoKeys = dataWithoutIgnored.mapObject(obj => JsonObject.fromMap(obj.toMap.filterNot { case (key, _) => md.keys.contains(key) }))

          val newData = if(md.keyFields.forall(_.readOnly)) {
            dataWithNoKeys
          } else dataWithoutIgnored
          this.add(newData,true,true,Some(entity.get.indexOf(itemToDuplicate.id)+1))
        };
        case None => logger.warn("duplicating invalid object")
      }
      checkChanges()

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


      add(placeholder.asJson,true,true)
      checkChanges()
    }




    private def propagate[T](data: Json,f: (Widget => ((Json, JSONMetadata) => Future[T]))): Future[Seq[T]] = {

      val rows = data.seq(child.key)

      logger.debug(s"propagate current ids ${childWidgets.map(_.rowId.get)} $data")

      val out = Future.sequence(childWidgets.filterNot(_.deleted).map{ case cw =>


        val oldData = cw.data.get

        val newData = rows.find(r => metadata.exists(m => JSONID.fromData(r,m,false) == cw.rowId.get )).getOrElse(Json.obj())
        val d = oldData.deepMerge(newData)

        logger.debug(
          s"""
             |propagate
             |field: ${field.name}
             |olddata:
             |$oldData
             |
             |newdata:
             |$newData
             |
             |result:
             | $d""".stripMargin)

        f(cw.widget)(d, metadata.get).map{ r =>
          r
        }
      }.toSeq)
      //correct futures
      out
    }

    def collectData(data:Json)(jsChilds:Seq[Json]) = {
      logger.debug("child collect data" + Map(child.key -> jsChilds.asJson).asJson.toString())
      data.deepMerge(Map(child.key -> jsChilds.asJson).asJson)
    }


    override def beforeSave(data: Json, metadata: JSONMetadata) = {
      logger.debug("Before save")
      propagate(data, _.beforeSave).map(collectData(data))
    }

    override def killWidget(): Unit = {
      super.killWidget()
      childWidgets.foreach(_.widget.killWidget())
    }


    override def afterRender() = Future.sequence(childWidgets.map(_.widget.afterRender())).map(_.forall(x => x))


    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = render(false,nested)

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = render(true,nested)



    var propListener:Registration = null

    def registerListener(immediate:Boolean) {
      propListener = prop.listen(propData => {
        childWidgets.foreach(_.widget.killWidget())
        childWidgets.foreach(_.changedListener.cancel())
        childWidgets.foreach(_.data.set(Json.Null)) // Fixes memory leakage on childs
        childWidgets.clear()
        entity.clear()
        val entityData = splitJson(propData)




        entityData.foreach { x =>
          val isOpen:Boolean = services.clientSession.isTableChildOpen(ClientSession.TableChildElement(
            field.name,
            metadata.map(_.objId).getOrElse(UUID.randomUUID()),
            metadata.flatMap(m => JSONID.fromData(x,m,false))
          ))
          add(x, isOpen,false)
        }

        for(i <- 0 until (min - entityData.length)) yield {
          logger.info(i.toString)
          metadata.map { m =>
            addItem(child, m)
          }
        }

        resetChangeAlert()

      }, immediate)
    }

    registerListener(true)


    def addButton(write:Boolean,m:JSONMetadata) = {
      val name = widgetParam.field.label.getOrElse(widgetParam.field.name)
      if (write && !disableAdd) {
        autoRelease(showIf(entity.transform(e => max.forall(_ > e.length))) {
          a(id := TestHooks.addChildId(m.objId),
            ClientConf.style.childAddButton, BootstrapStyles.Float.right(),
            onclick :+= addItemHandler(child,m),
            name,span(ClientConf.style.field,Icons.plusFill)
          ).render
        })
      } else frag()
    }

    def upButton(write:Boolean,widget: ChildRow,m:JSONMetadata) = {

      if (write && findItem(widget).get._2 > 0) {
        autoRelease(showIf(entity.transform(_.length > 1)) {
          div(
            BootstrapStyles.Grid.row,ClientConf.style.field,
            div(BootstrapCol.md(12),textAlign.center,
                a(ClientConf.style.childMoveButton,
                  onclick :+= up(widget),
                  i(UdashIcons.FontAwesome.Solid.caretUp),
                )
            )
          ).render
        })
      } else frag()
    }
    def downButton(write:Boolean,widget: ChildRow,m:JSONMetadata) = {

      if (write && findItem(widget).get._2 < childWidgets.length - 1) {
        autoRelease(showIf(entity.transform(_.length > 1)) {
          div(
            BootstrapStyles.Grid.row,ClientConf.style.field,
            div(BootstrapCol.md(12),textAlign.center,
                a(ClientConf.style.childMoveButton,
                  onclick :+= down(widget),
                  i(UdashIcons.FontAwesome.Solid.caretDown),
                )
            )
          ).render
        })
      } else frag()
    }

    def removeButton(write:Boolean,widget: ChildRow,m:JSONMetadata) = {

      if (write && !disableRemove) {
        autoRelease(showIf(entity.transform(_.length > min)) {
          div(
            BootstrapStyles.Grid.row,ClientConf.style.field,
            div(BootstrapCol.md(12),
              div(BootstrapStyles.Float.right(),
                a(ClientConf.style.childRemoveButton,
                  onclick :+= removeItem(widget),
                  Icons.minusFill,
                  id.bind(widget.rowId.transform(x => TestHooks.deleteChildId(m.objId,x))))
              )
            )
          ).render
        })
      } else frag()
    }


  }




}