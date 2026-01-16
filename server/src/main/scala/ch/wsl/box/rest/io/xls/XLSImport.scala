package ch.wsl.box.rest.io.xls

import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONMetadata}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.openxml4j.exceptions.InvalidFormatException

import java.io.{ByteArrayInputStream, File}
import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp, Date => SqlDate}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.{Try, Using}

object XLSImport {



  def xlsToJson(xls:Array[Byte],metadata:JSONMetadata): Try[Seq[Json]] = Try {

    Using.resource(new ByteArrayInputStream(xls)) { xls =>

      Using.resource(WorkbookFactory.create(xls)) { workbook =>
        val sheet = workbook.getSheetAt(0)


        val headerRow = Option(sheet.getRow(0))
          .getOrElse(throw new IllegalArgumentException(s"Header row not found at index 0"))

        val headerIndex = buildHeaderIndex(headerRow)

        // Map each ColumnSpec -> excel column index
        val excelIndexes: Seq[(Int, JSONField)] = metadata.fields.flatMap { c =>
          headerIndex.get(c.title).map(i => i -> c)
        }


        val rows = 1 to sheet.getLastRowNum
        rows.flatMap { rowIndex =>
          val row = sheet.getRow(rowIndex)
          if (row != null && !isRowEmpty(row)) Some {
            bindRow( row, excelIndexes)
          } else None
        }
      }
    }
  }


  // ---- Excel helpers ----

  private def buildHeaderIndex(headerRow: Row): Map[String, Int] = {
    val fmt = new DataFormatter()
    val m = scala.collection.mutable.Map.empty[String, Int]
    val lastCell = headerRow.getLastCellNum.toInt
    var c = 0
    while (c < lastCell) {
      val cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
      if (cell != null) {
        val v = fmt.formatCellValue(cell).trim
        if (v.nonEmpty) m += (v -> c)
      }
      c += 1
    }
    m.toMap
  }

  private def isRowEmpty(row: Row): Boolean = {
    val lastCell = row.getLastCellNum.toInt
    var c = 0
    while (c < lastCell) {
      val cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
      if (cell != null && cell.getCellType != CellType.BLANK) {
        val fmt = new DataFormatter()
        if (fmt.formatCellValue(cell).trim.nonEmpty) return false
      }
      c += 1
    }
    true
  }

  // ---- Binding / parsing ----

  private def bindRow(
                       row: Row,
                       excelIndexes: Seq[(Int,JSONField)]
                     )  = {

    val cells = excelIndexes.map{ case (i,field) =>
      val cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
      val valueOpt =  parseCell(cell, field)
      (valueOpt, field.nullable) match {
        case (None, false) =>
          throw new IllegalArgumentException(
            s"Non-nullable column '${field.title}' is empty at Excel row ${row.getRowNum + 1}"
          )
        case (None,true) => field.name -> Json.Null
        case (Some(v),_) => field.name -> v
      }
    }

    Json.fromFields(cells)

  }

  private def parseCell(cell: Cell, tpe: JSONField): Option[Json] = {
    if (cell == null) return None

    import ch.wsl.box.rest.utils.JSONSupport._

    val fmt = new DataFormatter()
    tpe.`type` match {
      case JSONFieldTypes.STRING =>
        val s = fmt.formatCellValue(cell).trim
        if (s.isEmpty) None else Some(Json.fromString(s))

      case JSONFieldTypes.INTEGER =>
        val n = parseNumber(cell, fmt).map(_.longValue)
        n.map { x =>
          Json.fromLong(x)
        }

      case JSONFieldTypes.NUMBER =>
        parseNumber(cell, fmt).flatMap(n => Json.fromDouble(n))

      case JSONFieldTypes.BOOLEAN =>
        val s = fmt.formatCellValue(cell).trim.toLowerCase
        if (s.isEmpty) None
        else {
          val b =
            s match {
              case "true" | "t" | "yes" | "y" | "1" => true
              case "false" | "f" | "no" | "n" | "0" => false
              case _ =>
                throw new IllegalArgumentException(s"Invalid boolean: '$s'")
            }
          Some(Json.fromBoolean(b))
        }

      case JSONFieldTypes.DATE =>
        parseAsDate(cell, fmt, ZoneId.systemDefault()).map(d => d.asJson)

      case JSONFieldTypes.DATETIME | JSONFieldTypes.DATETIMETZ =>
        parseAsInstant(cell, fmt, ZoneId.systemDefault()).map(ts => ts.asJson)
    }
  }

  private def parseNumber(cell: Cell, fmt: DataFormatter): Option[java.lang.Double] = {
    cell.getCellType match {
      case CellType.NUMERIC =>
        Some(java.lang.Double.valueOf(cell.getNumericCellValue))
      case CellType.STRING =>
        val s = cell.getStringCellValue.trim
        if (s.isEmpty) None else Some(java.lang.Double.valueOf(s.replace(",", ".")))
      case CellType.FORMULA =>
        cell.getCachedFormulaResultType match {
          case CellType.NUMERIC => Some(java.lang.Double.valueOf(cell.getNumericCellValue))
          case CellType.STRING =>
            val s = fmt.formatCellValue(cell).trim
            if (s.isEmpty) None else Some(java.lang.Double.valueOf(s.replace(",", ".")))
          case _ => None
        }
      case CellType.BLANK => None
      case _ =>
        val s = fmt.formatCellValue(cell).trim
        if (s.isEmpty) None else Some(java.lang.Double.valueOf(s.replace(",", ".")))
    }
  }


  private def parseAsInstant(cell: Cell, fmt: DataFormatter, zoneId: ZoneId): Option[LocalDateTime] = {
    if (cell.getCellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      val dt = cell.getDateCellValue.toInstant.atZone(zoneId).toLocalDateTime
      Some(dt)
    } else {
      val s = fmt.formatCellValue(cell).trim
      if (s.isEmpty) None
      else {
        // Try a few common timestamp patterns; extend as needed.
        val patterns = Seq(
          "yyyy-MM-dd HH:mm:ss",
          "yyyy-MM-dd'T'HH:mm:ss",
          "yyyy-MM-dd HH:mm",
          "yyyy-MM-dd"
        )
        val parsed = patterns.view.flatMap { p =>
          val f = DateTimeFormatter.ofPattern(p)
          Try {
            if (p == "yyyy-MM-dd") {
              LocalDate.parse(s, f).atStartOfDay(zoneId).toLocalDateTime
            } else {
              java.time.LocalDateTime.parse(s, f)
            }
          }.toOption
        }.headOption

        parsed.orElse {
          // Last resort: try Instant.parse (ISO-8601)
          Try(LocalDateTime.parse(s)).toOption
        }
      }
    }
  }

  private def parseAsDate(cell: Cell, fmt: DataFormatter, zoneId: ZoneId): Option[LocalDate] = {
    if (cell.getCellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      val inst = cell.getDateCellValue.toInstant
      Some(inst.atZone(zoneId).toLocalDate)
    } else {
      val s = fmt.formatCellValue(cell).trim
      if (s.isEmpty) None
      else {
        val patterns = Seq("yyyy-MM-dd", "dd.MM.yyyy", "MM/dd/yyyy")
        patterns.view.flatMap { p =>
          val f = DateTimeFormatter.ofPattern(p)
          Try(LocalDate.parse(s, f)).toOption
        }.headOption
      }
    }
  }
}
