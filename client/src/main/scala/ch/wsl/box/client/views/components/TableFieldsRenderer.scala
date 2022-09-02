package ch.wsl.box.client.views.components

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.{EntityFormState, EntityTableState}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONID, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom
import scalacss.ScalatagsCss._
import org.scalajs.dom.{Element, Event}
import io.udash._
import scribe.Logging
import scalatags.JsDom.TypedTag

/**
  * Created by andre on 5/2/2017.
  */
object TableFieldsRenderer extends Logging{

  import io.circe.syntax._

  import scalatags.JsDom.all._



  def toggleEdit(editing:Property[Boolean]) = {
    logger.info("Toggle edit")
    editing.set(!editing.get)
  }

  def renderLongText(string: String):Modifier = {
    val length = ClientConf.tableMaxTextLength
    val noHTML = typings.striptags.mod.apply(string)
    if(noHTML.length <= length) {
      p(noHTML)
    } else {
      val showAll = Property(false)
      p(
        noHTML.take(length),
        showIfElse(showAll)(
          span(noHTML.substring(length)," ",a(Labels.table.showLess,onclick :+= ((e:Event) => showAll.set(false)))).render,
          span("... ",a(Labels.table.showMore,onclick :+= ((e:Event) => showAll.set(true)))).render
        )
      )
    }
  }

  def apply(value:String, field:JSONField, keys:JSONID, routes:Routes):TypedTag[Element] = {


    val contentFixed = (field.lookup,field.widget) match {
      case (Some(opts),_) => {
        val label: String = opts.allLookup.find(_.id.string == value).orElse(opts.lookup.find(_.id.string == value)).map(_.value).getOrElse(value)
        val finalLabel = if(label.trim.length > 0) label else value
        p(finalLabel)
//        a(href := routes.edit(JSONKeys.fromMap(Map(field.key -> value)).asString).url,finalLabel)
      }
      case (None,Some(WidgetsNames.richTextEditor)) => renderLongText(value)
      case (None,_) => p(ClientConf.style.preformatted,value)
    }

//    val editing = Property(false)
//    val model = Property(value.asJson)
//
//    div(onclick :+= ((ev: Event) => toggleEdit(editing), true),
//      showIf(editing.transform(x => !x))(contentFixed.render),
//      showIf(editing)(div(JSONSchemaRenderer.fieldRenderer(field,model,keys.keys.map(_.key),false)).render)
//    )

    def align = field.`type` match{
      case JSONFieldTypes.NUMBER => if (field.lookup.isEmpty) ClientConf.style.numberCells else ClientConf.style.lookupCells
      case JSONFieldTypes.DATE | JSONFieldTypes.DATETIME | JSONFieldTypes.TIME => ClientConf.style.dateCells
      case _ => ClientConf.style.textCells
    }

    div(align)(contentFixed)


  }
}
