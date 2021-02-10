package ch.wsl.box.client.views.components.widget

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}

import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.datepicker.UdashDatePicker
import io.udash._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe._
import io.circe.syntax._
import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.udash.bootstrap.datepicker.UdashDatePicker.Placement
import io.udash.properties.single.Property
import org.scalajs.dom.{Event, KeyboardEvent}
import scalacss.internal.StyleA
import scalatags.JsDom
import scribe.Logging
import typings.flatpickr.optionsMod.{DateOption, Hook}
import typings.std.HTMLInputElement

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

trait DateTimeWidget[T] extends Widget  with Logging{

  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._


  val fieldType:FieldTypes.FieldType
  val dateTimeFormatters:DateTimeFormatters[T]


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

  protected def toDate(jsonDate:Json,range:Boolean):Array[DateOption] = {
    logger.info(s"toDate $jsonDate")
    if(jsonDate == Json.Null) return Array()
    val str = jsonDate.string.trim
    if(str == "") return Array()


    def toDateOption(d:String):DateOption = d

    val result = if(range) {
      val tokens = str.split("to").map(_.trim)
      if(tokens.length > 1)
        tokens.flatMap(t => strToTime(t,false))
      else
        strToTime(str,true)
    } else {
      strToTime(str,false)
    }

    result.map(toDateOption)


  }


  protected def showMe(modelLabel:String, model:Property[Json]):Modifier = autoRelease(WidgetUtils.showNotNull(model){ p =>
    div(if (modelLabel.length > 0) label(modelLabel) else {},
      div(BootstrapStyles.Float.right(), bind(model.transform(_.string))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  protected def editMe(id:Property[Option[String]], field:JSONField, model:Property[Json],style:StyleA, range:Boolean):Modifier = {

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    var flatpicker:typings.flatpickr.instanceMod.Instance = null

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

    val picker = input(
      style,if(field.nullable) {} else ClientConf.style.notNullable,
      onkeydown := { (e: KeyboardEvent) =>
        e.stopPropagation()
        e.keyCode match {
          case 13 => {
            handleDate(e.target.asInstanceOf[HTMLInputElement].value.asJson, true)
            flatpicker.close()
          }
          case _ => {}
        }
      },
      onblur := { (e: Event) =>
        e.stopPropagation()
        handleDate(e.target.asInstanceOf[HTMLInputElement].value.asJson, true)
      },
    ).render
    var changeListener:Registration = null



    def setListener(immediate: Boolean, flatpicker:typings.flatpickr.instanceMod.Instance) = {
      changeListener = model.listen({ d =>
        logger.info(s"Changed model to $d")
        handleDate(d)
      },immediate)
    }


    val result = div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      if (field.title.length > 0) WidgetUtils.toLabel(field, false) else {},
      tooltip(picker),
      div(BootstrapStyles.Visibility.clearfix)
    ).render

    val onChange:Hook = (
                          selectedDates:js.Array[typings.flatpickr.globalsMod.global.Date],
                          dateStr:String,
                          instance: typings.flatpickr.instanceMod.Instance,
                          data:js.UndefOr[js.Any]) => {
      changeListener.cancel()
      logger.info(s"flatpickr on change $dateStr, selectedDates: $selectedDates $instance $data")
      model.set(dateStr.asJson)
      setListener(false, instance)
    }

    val options = typings.flatpickr.optionsMod.Options()
      .setAllowInput(true)
      .setOnChange(onChange)

    if(range) {
      options.setMode(typings.flatpickr.flatpickrStrings.range)
    }

    fieldType match {
      case FieldTypes.DateTime => options.setEnableTime(true).setTime_24hr(true)
      case FieldTypes.Date => {}
      case FieldTypes.Time => options.setEnableTime(true).setTime_24hr(true).setNoCalendar(true).setDateFormat("H:i")
    }

    flatpicker = typings.flatpickr.mod.default(picker,options)

    setListener(true,flatpicker)


    result

  }
}


object DateTimeWidget {

  trait DateTimeWdg extends DateTimeWidget[LocalDateTime] {
    override val fieldType = FieldTypes.DateTime
    override val dateTimeFormatters: DateTimeFormatters[LocalDateTime] = DateTimeFormatters.timestamp
  }

  trait TimeWdg extends DateTimeWidget[LocalTime] {
    override val fieldType = FieldTypes.Time
    override val dateTimeFormatters: DateTimeFormatters[LocalTime] = DateTimeFormatters.time
  }

  trait DateWdg extends DateTimeWidget[LocalDate] {
    override val fieldType = FieldTypes.Date
    override val dateTimeFormatters: DateTimeFormatters[LocalDate] = DateTimeFormatters.date
  }



  case class Date(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends DateWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePicker,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object Date extends ComponentWidgetFactory {
    
    override def name: String = WidgetsNames.datepicker

    override def create(params: WidgetParams): Widget = Date(params.id,params.field,params.prop)
  }

  case class DateTime(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends DateTimeWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePicker,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object DateTime extends ComponentWidgetFactory {

    override def name: String = WidgetsNames.datetimePicker
    
    override def create(params: WidgetParams): Widget = DateTime(params.id,params.field,params.prop)
  }

  case class Time(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends TimeWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePicker,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object Time extends ComponentWidgetFactory {

    override def name: String = WidgetsNames.timepicker
    
    override def create(params: WidgetParams): Widget = Time(params.id,params.field,params.prop)
  }

  case class DateFullWidth(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends DateWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePickerFullWidth,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object DateFullWidth extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datepickerFullWidth
    override def create(params: WidgetParams): Widget = DateFullWidth(params.id,params.field,params.prop)
  }

  case class DateTimeFullWidth(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends DateTimeWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePickerFullWidth,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object DateTimeFullWidth extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datepickerFullWidth
    override def create(params: WidgetParams): Widget = DateTimeFullWidth(params.id,params.field,params.prop)
  }

  case class TimeFullWidth(id: Property[Option[String]], field: JSONField, prop: Property[Json], range:Boolean = false) extends TimeWdg {
    override def edit() = editMe(id,field,prop,ClientConf.style.dateTimePickerFullWidth,range)
    override protected def show(): JsDom.all.Modifier = showMe(field.title,prop)
  }

  object TimeFullWidth extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.timepickerFullWidth
    override def create(params: WidgetParams): Widget = TimeFullWidth(params.id,params.field,params.prop)
  }



}
