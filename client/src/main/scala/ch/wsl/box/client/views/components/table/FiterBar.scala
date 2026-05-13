package ch.wsl.box.client.views.components.table

import ch.wsl.box.client.services.{ClientConf, Labels, UI}
import ch.wsl.box.client.views.FieldQuery
import ch.wsl.box.client.views.components.widget.DateTimeWidget
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import ch.wsl.typings.choicesJs.anon.PartialOptions
import ch.wsl.typings.choicesJs.publicTypesSrcScriptsInterfacesInputChoiceMod.InputChoice
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.udash.bindings.modifiers.Binding
import io.udash._
import org.scalajs.dom._
import scribe.Logging

import scala.scalajs.js
import scala.util.Try
import scala.scalajs.js.JSConverters.JSRichIterableOnce

trait FilterBar extends Logging {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import ch.wsl.box.client.Context._
  import Implicits._


  def fieldQueries:Property[Seq[FieldQuery]]
  def lookups:ReadableProperty[Seq[JSONLookups]]

  def filterOptions(metadata:JSONMetadata, field:String, operator: Property[String])(mod:Modifier*) = {


    def label = (id:String) => id match {
      case Filter.FK_NOT => StringFrag(Labels.filter.not)
      case Filter.FK_EQUALS => StringFrag(Labels.filter.equals)
      case Filter.FK_LIKE => StringFrag(Labels.filter.contains)
      case Filter.FK_DISLIKE => StringFrag(Labels.filter.without)
      case Filter.LIKE => StringFrag(Labels.filter.contains)
      case Filter.DISLIKE => StringFrag(Labels.filter.without)
      case Filter.BETWEEN => StringFrag(Labels.filter.between)
      case Filter.< => StringFrag(Labels.filter.lt)
      case Filter.> => StringFrag(Labels.filter.gt)
      case Filter.<= => StringFrag(Labels.filter.lte)
      case Filter.>= => StringFrag(Labels.filter.gte)
      case Filter.EQUALS => StringFrag(Labels.filter.equals)
      case Filter.IN => StringFrag(Labels.filter.in)
      case Filter.NONE => StringFrag(Labels.filter.none)
      case Filter.NOTIN => StringFrag(Labels.filter.notin)
      case Filter.NOT => StringFrag(Labels.filter.not)
      case _ => StringFrag(id)
    }

    val options = SeqProperty{
      metadata.fields.find(_.name == field).toSeq.flatMap(f => UI.enabledFilters(Filter.options(f)))
    }

    Select(operator, options)(label,mod)

  }


  def filterField(filterValue: Property[String], field:JSONField, filterOperator:String,nested:Binding.NestedInterceptor)(mod:Modifier*):Modifier = {

    filterValue.listen(v => logger.info(s"Filter for ${field.name} changed in: $v"))



    def filterFieldStd = field.`type` match {
      case JSONFieldTypes.TIME => DateTimeWidget.Time(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null)).edit(nested)
      case JSONFieldTypes.DATE => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
      case JSONFieldTypes.DATETIME => ClientConf.filterPrecisionDatetime match{
        case JSONFieldTypes.DATE => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
        case _ => DateTimeWidget.DateTime(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
      }
      case JSONFieldTypes.DATETIMETZ => ClientConf.filterPrecisionDatetime match{
        case JSONFieldTypes.DATE => DateTimeWidget.Date(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
        case _ => DateTimeWidget.DateTimeTZ(Property(None),JSONField.fullWidth,filterValue.bitransform(_.asJson)(_.string),Property(Json.Null),true).edit(nested)
      }
      case JSONFieldTypes.NUMBER | JSONFieldTypes.INTEGER if field.widget.contains(WidgetsNames.integerDecimal2) && !Seq(Filter.BETWEEN, Filter.IN, Filter.NOTIN).contains(filterOperator) => {
        if(Try(filterValue.get.toDouble).toOption.isEmpty) filterValue.set("")
        val properyNumber = Property("")
        filterValue.sync(properyNumber)(_.toDoubleOption.map(_ / 100).map(_.toString).getOrElse(""),_.toDoubleOption.map(_ * 100).map(_.toString).getOrElse("") )
        NumberInput(properyNumber)(mod)
      }
      case JSONFieldTypes.NUMBER | JSONFieldTypes.INTEGER if field.lookup.isEmpty && !Seq(Filter.BETWEEN, Filter.IN, Filter.NOTIN).contains(filterOperator) => {
        if(Try(filterValue.get.toDouble).toOption.isEmpty) filterValue.set("")
        NumberInput(filterValue)(mod)
      }
      case _ => TextInput(filterValue)(mod)
    }

    def filterFieldLookup(lookup:JSONFieldLookup) = {
      def choises(lookups:JSONLookups):Seq[InputChoice] = lookup match {
        case JSONFieldLookupRemote(lookupEntity, map, lookupQuery) => {
          lookups.lookups.map(l => InputChoice(l.value,l.id.string))
        }
        case JSONFieldLookupExtractor(extractor) => Seq()
        case JSONFieldLookupData(data) => data.map(x => InputChoice(x.value,x.id.string))
      }

      val el = select().render


      val observer = new MutationObserver({ (mutations, observer) =>
        observer.disconnect()
        val options = PartialOptions()
          .setRemoveItemButton(true)
          .setShouldSort(false)
          .setItemSelectText("")
        val choicesJs = new ch.wsl.typings.choicesJs.mod.default(el, options)
        el.addEventListener("change", (e: Event) => {
          (choicesJs.getValue(true): Any) match {
            case list: js.Array[String] => println(list)
            case a: String => filterValue.set(a)
            case _ => filterValue.set("")
          }
        })

        lookups.listen({l =>
          l.find(_.fieldName == field.name).foreach{ fl =>
            choicesJs.clearChoices()
            val c = choises(fl)
            choicesJs.asInstanceOf[js.Dynamic].setChoices(c.toJSArray)
            if(filterValue.get.nonEmpty) {
              choicesJs.setChoiceByValue(filterValue.get)
            }
          }

        },true)

        filterValue.listen{ fv =>
          if(fv.isEmpty && choicesJs.getValue(true).toString.nonEmpty) {
            choicesJs.clearStore()
          }
        }

      })
      observer.observe(document,MutationObserverInit(childList = true, subtree = true))
      el
    }

    field.lookup match {
      case Some(value) => filterFieldLookup(value)
      case None => filterFieldStd
    }

  }


  def filterPropsField(_field:JSONField) = {
    val fieldQuery: Property[Option[FieldQuery]] = fieldQueries.bitransform(_.find(_.field.name == _field.name)) { el =>
      fieldQueries.get.map { old =>
        if (old.field.name == _field.name && el.isDefined) el.get else old
      }
    }
    val filterValue: Property[String] = fieldQuery.bitransform(_.map(_.filterValue).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterValue = value)))
    val operator: Property[String] = fieldQuery.bitransform(_.map(_.filterOperator).getOrElse(""))(value => fieldQuery.get.map(x => x.copy(filterOperator = value)))
    (filterValue,operator)
  }

  def render(columns:Seq[JSONField],metadata:JSONMetadata):HTMLElement

}