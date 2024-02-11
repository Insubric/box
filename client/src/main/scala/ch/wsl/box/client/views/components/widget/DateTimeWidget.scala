package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.Context

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.datepicker.UdashDatePicker
import io.udash._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe._
import io.circe.syntax._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.datepicker.UdashDatePicker.Placement
import io.udash.properties.single.Property
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom.{Event, KeyboardEvent}
import scalacss.internal.StyleA
import scalatags.JsDom
import scribe.Logging
import ch.wsl.typings.flatpickr.anon.kinkeyCustomLocaledefault
import ch.wsl.typings.flatpickr.distTypesLocaleMod.{CustomLocale, Locale}
import ch.wsl.typings.flatpickr.distTypesOptionsMod.Hook
import ch.wsl.typings.flatpickr.mod.flatpickr.Options.DateOption

import java.util
import scala.scalajs.js
import scala.util.Try
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

object FieldTypes {
  sealed trait FieldType
  case object DateTime extends FieldType
  case object Date extends FieldType
  case object Time extends FieldType
}

trait DateTimeWidget[T] extends Widget with HasData with Logging{

  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._


  val fieldType:FieldTypes.FieldType
  val dateTimeFormatters:DateTimeFormatters[T]
  val data:Property[Json]
  val id:ReadableProperty[Option[String]]
  val range:Boolean
  val allData:ReadableProperty[Json]

  val format = field.params.flatMap(_.getOpt("format"))

  val defaultFrom = field.params.flatMap(_.getOpt("defaultFrom"))

  val fullWidth = field.params.exists(_.js("fullWidth") == true.asJson)

  private val locale: CustomLocale = {
    val l = Context.services.clientSession.lang() match {
      case "it" => ch.wsl.typings.flatpickr.distL10nItMod.default.it.get
      case "fr" => ch.wsl.typings.flatpickr.distL10nFrMod.default.fr.get
      case "de" => ch.wsl.typings.flatpickr.distL10nDeMod.default.de.get
      case _ => ch.wsl.typings.flatpickr.distL10nMod.default.default
    }
    l.setFirstDayOfWeek(1)
    l.setRangeSeparator(" → ")
    l
  }

  override def edit(nested:Binding.NestedInterceptor) = editMe()
  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = showMe(field.title,nested)

  val formatted:ReadableProperty[String] = data.transform{ js =>
    dateTimeFormatters.parse(js.string).map(v => dateTimeFormatters.format(v,format) ).getOrElse("")
  }

  private def strToTime(s:String,r:Boolean): Array[String] = {

    dateTimeFormatters.parse(s).toSeq.flatMap{ parsed =>
      val timestamp = dateTimeFormatters.format(parsed)
      (r,s.length,fieldType) match {
        case (_,_,FieldTypes.Time) => Array(timestamp)
        case (false, _,_) => Array(timestamp)
        case (true, x,_) if x > 7 => Array(timestamp)
        case (true, x,_) if x > 4 => { //month interval
          logger.info(s"Expand month")
          val nextMonth = dateTimeFormatters.format(dateTimeFormatters.nextMonth(parsed))
          Array(timestamp,nextMonth)
        }
        case (true, x,_) => { //year interval
          logger.info(s"Expand year")
          val nextYear = dateTimeFormatters.format(dateTimeFormatters.nextYear(parsed))
          Array(timestamp,nextYear)
        }
      }
    }
  }.toArray //cannot do toArray directly because array need the typetag to be constructed

  def toDateOption(d:String):DateOption = d

  protected def toDate(jsonDate:Json,range:Boolean):Array[DateOption] = {
    logger.info(s"toDate $jsonDate")
    if(jsonDate == Json.Null) return Array()
    val str = jsonDate.string.trim
    if(str == "") return Array()




    val result = if(range) {

      val tokens = str.split(locale.rangeSeparator.getOrElse("to")).map(_.trim)
      if(tokens.length > 1)
        tokens.flatMap(t => strToTime(t,false))
      else
        strToTime(str,true)
    } else {
      strToTime(str,false)
    }

    result.map(toDateOption)


  }


  override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
    format match {
      case Some(formatter) => nested(bind(data.transform{js =>

        val locale = Context.services.clientSession.lang() match {
          case "it" => java.util.Locale.ITALIAN
          case "de" => java.util.Locale.GERMAN
          case "fr" => java.util.Locale.FRENCH
          case _ => java.util.Locale.ENGLISH
        }

        dateTimeFormatters.parse(js.string)
          .map{x => dateTimeFormatters.format(x,Some(formatter),Some(locale))}
          .getOrElse(js.string)
      }))
      case None => nested(bind(data.transform(_.string)))
    }
  }

  protected def showMe(modelLabel:String,nested:Binding.NestedInterceptor):Modifier = autoRelease(WidgetUtils.showNotNull(data,nested){ p =>
    div(ClientConf.style.smallBottomMargin, if (modelLabel.length > 0) label(modelLabel) else {},
      div(BootstrapStyles.Float.right(), nested(bind(formatted))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  protected def editMe() = {

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      if (field.title.length > 0) WidgetUtils.toLabel(field,WidgetUtils.LabelRight, false) else {},
      tooltip(picker(fullWidth))._1,
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }


  override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = picker(true)

  protected def picker(fullwidth:Boolean):HTMLInputElement = {



    var flatpicker:ch.wsl.typings.flatpickr.mod.flatpickr.Instance = null

    def handleDate(d:Json,force:Boolean = false): Unit = {
      if(range) {
        val dates = toDate(d,range).toJSArray
        flatpicker.setDate(dates,force)
      } else {
        toDate(d,range).headOption match {
          case Some(date) => flatpicker.setDate(date,force)
          case None => flatpicker.clear(force)
        }
      }
    }

    val style = fullwidth match {
      case true => ClientConf.style.dateTimePickerFullWidth
      case false => ClientConf.style.dateTimePicker
    }

    val picker = input(
      color.black,
      style,WidgetUtils.toNullable(field.nullable),
      onkeydown := { (e: KeyboardEvent) =>
        e.keyCode match {
          case 13 => {
            handleDate(e.target.asInstanceOf[HTMLInputElement].value.asJson, true)
            flatpicker.close()
          }
          case _ => {}
        }
      },
      onclick := { (e:Event) =>
        if(data.get == Json.Null) {
          val defaultDate = for{
            field <- defaultFrom
            jsDate <- allData.get.jsOpt(field)
          } yield jsDate

          defaultDate match {
            case Some(date) => handleDate(date,true)
            case None => data.set(dateTimeFormatters.format(dateTimeFormatters.from(new java.util.Date().getTime)).asJson)
          }
        }
      },
      onblur := { (e: Event) =>
        e.preventDefault()
        handleDate(e.target.asInstanceOf[HTMLInputElement].value.asJson, true)
      },
    ).render
    var changeListener:Registration = null



    def setListener(immediate: Boolean, flatpicker:ch.wsl.typings.flatpickr.distTypesInstanceMod.Instance) = {
      changeListener = data.listen({ d =>
        logger.info(s"Changed model to $d")
        handleDate(d)
      },immediate)
    }



    //scala.scalajs.js.Function4[scala.scalajs.js.Array[ch.wsl.typings.flatpickr.globalsMod.global.Date],String,ch.wsl.typings.flatpickr.instanceMod.Instance,Any | Unit,Unit]
    val onChange:Hook = (
                          selectedDates:js.Array[ch.wsl.typings.flatpickr.distTypesGlobalsMod.global.Date],
                          dateStr:String,
                          instance: ch.wsl.typings.flatpickr.distTypesInstanceMod.Instance,
                          _data:js.UndefOr[Any]) => {
      changeListener.cancel()
      logger.info(s"flatpickr on change $dateStr, selectedDates: $selectedDates $instance ${_data}")
      if(dateStr == "") {
        data.set(Json.Null)
      } else {
        data.set(dateStr.asJson)
      }
      setListener(false, instance)
    }





    val options = ch.wsl.typings.flatpickr.distTypesOptionsMod.Options()
      .setAllowInput(true)
      .setDisableMobile(true)
      .setLocale(locale)
      .setOnChange(onChange)

    if(range) {
      options.setMode(ch.wsl.typings.flatpickr.flatpickrStrings.range)
    }

    fieldType match {
      case FieldTypes.DateTime => options.setEnableTime(true).setTime_24hr(true)
      case FieldTypes.Date => options.setEnableTime(false).setNoCalendar(false)
      case FieldTypes.Time => options.setEnableTime(true).setTime_24hr(true).setNoCalendar(true).setDateFormat("H:i")
    }

    flatpicker = ch.wsl.typings.flatpickr.mod.default(picker,options)

    setListener(true,flatpicker)


    picker

  }
}


object DateTimeWidget {


  case class Date(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], allData: ReadableProperty[Json], range:Boolean = false) extends DateTimeWidget[LocalDate] {
    override val fieldType = FieldTypes.Date
    override val dateTimeFormatters: DateTimeFormatters[LocalDate] = DateTimeFormatters.date
  }

  object Date extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datepicker
    override def create(params: WidgetParams): Widget = Date(params.id,params.field,params.prop,params.allData)
  }



  case class DateTime(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], allData: ReadableProperty[Json], range:Boolean = false) extends DateTimeWidget[LocalDateTime] {
    override val fieldType = FieldTypes.DateTime
    override val dateTimeFormatters: DateTimeFormatters[LocalDateTime] = DateTimeFormatters.timestamp
  }

  object DateTime extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datetimePicker
    override def create(params: WidgetParams): Widget = DateTime(params.id,params.field,params.prop,params.allData)
  }


  object Time extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.timepicker
    override def create(params: WidgetParams): Widget = Time(params.id,params.field,params.prop,params.allData)
  }

  case class Time(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], allData: ReadableProperty[Json], range:Boolean = false) extends DateTimeWidget[LocalTime] {
    override val fieldType = FieldTypes.Time
    override val dateTimeFormatters: DateTimeFormatters[LocalTime] = DateTimeFormatters.time
  }




}
