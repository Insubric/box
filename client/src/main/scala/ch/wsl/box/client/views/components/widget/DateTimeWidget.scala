package ch.wsl.box.client.views.components.widget

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}

import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.datepicker.UdashDatePicker
import io.udash._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe._
import io.circe.syntax._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
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

trait DateTimeWidget[T] extends Widget with HasData with Logging{

  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._


  val fieldType:FieldTypes.FieldType
  val dateTimeFormatters:DateTimeFormatters[T]
  val data:Property[Json]
  val id:ReadableProperty[Option[String]]
  val range:Boolean

  val format = field.params.flatMap(_.getOpt("format"))

  val fullWidth = field.params.exists(_.js("fullWidth") == true.asJson)
  val style = fullWidth match {
    case true => ClientConf.style.dateTimePickerFullWidth
    case false => ClientConf.style.dateTimePicker
  }
  override def edit() = editMe(id,field,data,style,range)
  override protected def show(): JsDom.all.Modifier = showMe(field.title)

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


  protected def showMe(modelLabel:String):Modifier = autoRelease(WidgetUtils.showNotNull(data){ p =>
    div(ClientConf.style.smallBottomMargin, if (modelLabel.length > 0) label(modelLabel) else {},
      div(BootstrapStyles.Float.right(), bind(formatted)),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  protected def editMe(id:ReadableProperty[Option[String]], field:JSONField, model:Property[Json],style:StyleA, range:Boolean):Modifier = {

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
      style,WidgetUtils.toNullable(field.nullable),
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
      onclick := { (e:Event) =>
        if(data.get == Json.Null) {
          data.set(dateTimeFormatters.format(dateTimeFormatters.from(new java.util.Date().getTime)).asJson)
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
      if(dateStr == "") {
        model.set(Json.Null)
      } else {
        model.set(dateStr.asJson)
      }
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
      case FieldTypes.Date => options.setEnableTime(false).setNoCalendar(false)
      case FieldTypes.Time => options.setEnableTime(true).setTime_24hr(true).setNoCalendar(true).setDateFormat("H:i")
    }

    flatpicker = typings.flatpickr.mod.default(picker,options)

    setListener(true,flatpicker)


    result

  }
}


object DateTimeWidget {


  case class Date(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], range:Boolean = false) extends DateTimeWidget[LocalDate] {
    override val fieldType = FieldTypes.Date
    override val dateTimeFormatters: DateTimeFormatters[LocalDate] = DateTimeFormatters.date
  }

  object Date extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datepicker
    override def create(params: WidgetParams): Widget = Date(params.id,params.field,params.prop)
  }



  case class DateTime(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], range:Boolean = false) extends DateTimeWidget[LocalDateTime] {
    override val fieldType = FieldTypes.DateTime
    override val dateTimeFormatters: DateTimeFormatters[LocalDateTime] = DateTimeFormatters.timestamp
  }

  object DateTime extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.datetimePicker
    override def create(params: WidgetParams): Widget = DateTime(params.id,params.field,params.prop)
  }


  object Time extends ComponentWidgetFactory {
    override def name: String = WidgetsNames.timepicker
    override def create(params: WidgetParams): Widget = Time(params.id,params.field,params.prop)
  }

  case class Time(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json], range:Boolean = false) extends DateTimeWidget[LocalTime] {
    override val fieldType = FieldTypes.Time
    override val dateTimeFormatters: DateTimeFormatters[LocalTime] = DateTimeFormatters.time
  }




}
