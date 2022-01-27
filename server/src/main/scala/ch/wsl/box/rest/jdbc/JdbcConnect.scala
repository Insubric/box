package ch.wsl.box.rest.jdbc

import java.sql._
import ch.wsl.box.model.boxentities.BoxExportField
import ch.wsl.box.model.boxentities.BoxExportField.BoxExportHeader_i18n_row
import ch.wsl.box.rest.utils.UserProfile
import io.circe.Json
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, TypeMapping, UserDatabase}
import ch.wsl.box.model.shared.{DataResult, DataResultTable}
import ch.wsl.box.services.Services


/**
  * A Scala JDBC connection example by Alvin Alexander,
  * http://alvinalexander.com
  */
object JdbcConnect extends Logging {

  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._


  def dynamicFunction(name:String, args: Seq[Json], lang:String)(implicit ec:ExecutionContext,up:UserProfile,services:Services):Future[Option[DataResultTable]] = {

    val result = Future{
      // make the connection
      val connection = services.connection.dbConnection.source.createConnection()
      val result = Try {
        connection.setAutoCommit(false)
        // create the statement, and run the select query
        val roleStatement = connection.createStatement()
        roleStatement.execute(s"""SET ROLE "${up.name}" """)

        val statement = connection.createStatement()
        val argsStr = if (args == null) ""
        else args.map(_.toString()).mkString(",")

        val query = s"SELECT ${services.connection.dbSchema}.$name($argsStr)".replaceAll("'", "\\'").replaceAll("\"", "'")
        logger.info(query)
        val dynResultSet = statement.executeQuery(query)
        val dynQuery = dynResultSet.getString(1)

        val dynStatement = connection.createStatement()
        val resultSet = dynStatement.executeQuery(dynQuery)
        connection.commit()
        val metadata = getColumnMeta(resultSet.getMetaData)
        val data = getResults(resultSet, metadata)
        DataResultTable(metadata.map(_.label),metadata.map(x => TypeMapping.jsonTypesMapping.getOrElse(x.datatype,"string")), data, Map())
      } match {
        case Failure(exception) => Some(DataResultTable(Seq("Database error"),Seq("string"),Seq(Seq(Json.fromString(exception.getMessage))),errorMessage = Some(exception.getMessage)))
        case Success(value) => Some(value)
      }
      connection.close()
      result
    }

    for{
      r <- result
      labels <- useI18nHeader(lang,r.toSeq.flatMap(_.headers))
    } yield r.map(_.copy(headers = labels))
  }

  def function(name:String, args: Seq[Json], lang:String)(implicit ec:ExecutionContext,up:UserProfile,services:Services):Future[Option[DataResultTable]] = {

    val result = Future{
      // make the connection
      val connection = services.connection.dbConnection.source.createConnection()
      val result = Try {
        connection.setAutoCommit(false)
        // create the statement, and run the select query
        val roleStatement = connection.createStatement()
        roleStatement.execute(s"""SET ROLE "${up.name}" """)

        val statement = connection.createStatement()
        val argsStr = if (args == null) ""
        else args.map(_.toString()).mkString(",")

        val query = s"SELECT * FROM ${services.connection.dbSchema}.$name($argsStr)".replaceAll("'", "\\'").replaceAll("\"", "'")
        logger.info(query)
        val resultSet = statement.executeQuery(query)
        connection.commit()
        val metadata = getColumnMeta(resultSet.getMetaData)
        val data = getResults(resultSet, metadata)
        DataResultTable(metadata.map(_.label),metadata.map(x => TypeMapping.jsonTypesMapping.getOrElse(x.datatype,"string")), data, Map())
      } match {
        case Failure(exception) => Some(DataResultTable(Seq("Database error"),Seq("string"),Seq(Seq(Json.fromString(exception.getMessage))),errorMessage = Some(exception.getMessage)))
        case Success(value) => Some(value)
      }
      connection.close()
      result
    }

    for{
      r <- result
      labels <- useI18nHeader(lang,r.toSeq.flatMap(_.headers))
    } yield r.map(_.copy(headers = labels))
  }


  // TODO @boris, could we user the default labels table instead creating a new one just for the export?
  private def useI18nHeader(lang:String,keys: Seq[String])(implicit ec:ExecutionContext,services:Services):Future[Seq[String]] = Future.sequence{
    keys.map{ key =>
      services.connection.adminDB.run(BoxExportField.BoxExportHeader_i18nTable.filter(e => e.key === key && e.lang === lang).result).map { label =>
        if(label.isEmpty) logger.warn(s"No translation for $key in $lang, insert translation in table export_header_i18n")
        label.headOption.map(_.label).getOrElse(key)
      }
    }
  }

  private case class ColumnMeta(index: Int, label: String, datatype: String)


  /**
    * Returns a list of columns for specified ResultSet which describes column properties we are interested in.
    */
  private def getColumnMeta(rsMeta: ResultSetMetaData): Seq[ColumnMeta] =
    (for {
      idx <- 1 to rsMeta.getColumnCount
      colName = rsMeta.getColumnLabel(idx).toLowerCase
      colType = rsMeta.getColumnClassName(idx)
    } yield ColumnMeta(idx, colName, colType)).toList

  /**
    * Creates a stream of results on top of a ResultSet.
    */
  private def getResults(rs: ResultSet, cols: Seq[ColumnMeta]): Seq[Seq[Json]] =
    new Iterator[Seq[Json]] {
      def hasNext = rs.next
      def next() = rowToObj(rs,cols)
    }.toStream.toList

  /**
    * Given a row from a ResultSet produces a JSON document.
    */
  private def rowToObj(rs: ResultSet, cols: Seq[ColumnMeta]):Seq[Json] = {
    for {
      ColumnMeta(index, label, datatype) <- cols
      value = columnValueGetter(datatype, index, rs)
    } yield value
  }

  /**
    * Takes a fully qualified Java type as String and returns one of the subtypes of JValue by fetching a value
    * from result set and converting it to proper type.
    * It supports only the most common types and everything else that does not match this conversion is converted
    * to String automatically. If you see that you results should contain more specific type instead of String
    * add conversion cases to {{{resultsetGetters}}} map.
    */
  private def columnValueGetter(datatype: String, columnIdx: Int, rs: ResultSet): Json = {
    val obj = rs.getObject(columnIdx)
    if (obj == null)
      Json.Null
    else {
      resultsetGetters(datatype,obj)
    }
  }

  private def resultsetGetters(datatype:String,obj:Object):Json = datatype match {
    case "java.lang.Integer" => obj.asInstanceOf[Int].asJson
    case "java.lang.Long" => obj.asInstanceOf[Long].asJson
    case "java.lang.Double" => obj.asInstanceOf[Double].asJson
    case "java.lang.Float" => obj.asInstanceOf[Float].asJson
    case "java.lang.BigDecimal" => obj.asInstanceOf[BigDecimal].asJson
    case "java.lang.Boolean" =>  obj.asInstanceOf[Boolean].asJson
    case "java.sql.Clob" => {
      val clob = obj.asInstanceOf[Clob]
      clob.getSubString(1, clob.length.toInt).asJson
    }
    case "java.lang.String" => obj.asInstanceOf[String].asJson
    case "java.sql.Timestamp" | "java.time.LocalDateTime" => obj.toString.asJson               //do not issue warnings for timestamp
    case "java.math.BigDecimal" => obj.toString.asJson                                         //do not issue warnings for BigDecimal
    case _ => {
      logger.warn(s"datatype: $datatype not found")
      obj.toString.asJson
    }
  }
}
