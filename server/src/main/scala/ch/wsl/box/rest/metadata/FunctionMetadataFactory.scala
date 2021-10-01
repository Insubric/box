package ch.wsl.box.rest.metadata

import java.util.UUID

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.boxentities.BoxExportField.{BoxExportField_i18n_row, BoxExportField_row}
import ch.wsl.box.model.boxentities.BoxFunction.{BoxFunctionField_i18n_row, BoxFunctionField_row}
import ch.wsl.box.model.boxentities.{BoxExport, BoxExportField}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.utils.UserProfile
import io.circe.Json
import io.circe.parser.parse
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class FunctionMetadataFactory(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,services:Services) extends Logging with DataMetadataFactory {

  import io.circe.generic.auto._

  implicit val db = up.db
  implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

  def functions = ch.wsl.box.model.boxentities.BoxFunction

  def list: Future[Seq[String]] = services.connection.adminDB.run{
    functions.BoxFunctionTable.result
  }.map{_.sortBy(_.order.getOrElse(Double.MaxValue)).map(_.name)}

  def list(lang:String): Future[Seq[ExportDef]] = {

    //    def accessibleExport = for {
    //      roles <- up.memberOf
    //      ex <- Export.Export.filter(ex => ex.access_role.isEmpty || ex.access_role inSet roles || roles.contains(up.name))
    //    } yield ex

    def checkRole(roles:List[String], access_roles:List[String], accessLevel:Int) =  roles.intersect(access_roles).size>0 || access_roles.isEmpty || access_roles.contains(up.name) || accessLevel == 1000

    def query    = for {
      (e, ei18) <- functions.BoxFunctionTable joinLeft(functions.BoxFunction_i18nTable.filter(_.lang === lang)) on(_.function_uuid === _.function_uuid)

    } yield (ei18.flatMap(_.label), e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip), e.access_role, e.mode)

    //    def queryResult = Auth.boxDB.run(query.result)


    for{
      al <- up.accessLevel
      qr <-  services.connection.adminDB.run(query.result)
    } yield {
      qr.filter(_._6.map(ar => checkRole(List(),ar, al)).getOrElse(true)) // TODO how to manage roles?
        .sortBy(_._3.getOrElse(Double.MaxValue)).map(
        { case (label, name, _, hint, tooltip, _, mode) =>
          ExportDef(name, label.getOrElse(name), hint, tooltip, mode)
        })

    }

  }

  def defOf(name:String, lang:String): Future[ExportDef] = {
    val query = for {
      (e, ei18) <- functions.BoxFunctionTable joinLeft(functions.BoxFunction_i18nTable.filter(_.lang === lang)) on(_.function_uuid === _.function_uuid)
      if e.name === name

    } yield (ei18.flatMap(_.label), e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip),e.mode)

    services.connection.adminDB.run{
      query.result
    }.map(_.map{ case (label, name, _, hint, tooltip, mode) =>
      ExportDef(name, label.getOrElse(name), hint, tooltip, mode)
    }.head)
  }

  def of(schema:String,name:String, lang:String):Future[JSONMetadata]  = {
    val queryExport = for{
      (func, functionI18n) <- functions.BoxFunctionTable joinLeft functions.BoxFunction_i18nTable.filter(_.lang === lang) on (_.function_uuid === _.function_uuid)
      if func.name === name

    } yield (func,functionI18n)

    def queryField(functionId:UUID) = for{
      (f, fi18n) <- functions.BoxFunctionFieldTable joinLeft functions.BoxFunctionField_i18nTable.filter(_.lang === lang) on (_.field_uuid === _.field_uuid)
      if f.function_uuid === functionId
    } yield (f, fi18n)

    for {
      (func, functionI18n)  <- services.connection.adminDB.run {
        queryExport.result
      }.map(_.head)

      fields <- services.connection.adminDB.run {
        queryField(func.function_uuid.get).sortBy(_._1.field_uuid).result
      }

      jsonFields <- Future.sequence(fields.map(fieldsMetadata(schema,lang)))

    } yield {

      if(functionI18n.isEmpty) logger.warn(s"Export ${func.name} (function_id: ${func.function_uuid}) has no translation to $lang")


      val layout = Layout.fromString(func.layout).getOrElse(Layout.fromFields(jsonFields))


      JSONMetadata(
        func.function_uuid.get,
        func.name,
        EntityKind.FUNCTION.kind,
        functionI18n.flatMap(_.label).getOrElse(name),
        jsonFields,
        layout,
        "function",
        lang,
        Seq(),
        Seq(),
        Seq(),
        NaturalKey,
        None,
        Seq(),
        None,
        FormActionsMetadata.default
      )
    }
  }

  private def fieldsMetadata(schema:String, lang:String)(el:(BoxFunctionField_row, Option[BoxFunctionField_i18n_row])):Future[JSONField] = {
    import ch.wsl.box.shared.utils.JSONUtils._

    val (field,fieldI18n) = el

    if(fieldI18n.isEmpty) logger.warn(s"Export field ${field.name} (export_id: ${field.field_uuid}) has no translation to $lang")


    val lookup: Future[Option[JSONFieldLookup]] = {for{
      entity <- field.lookupEntity
      value <- field.lookupValueField
      text <- fieldI18n.flatMap(_.lookupTextField)

    } yield {
      import io.circe.generic.auto._
      for {

        keys <- boxDb.adminDb.run(EntityMetadataFactory.keysOf(schema,entity))
        filter = {
            for{
              queryString <- field.lookupQuery
              queryJson <- parse(queryString).right.toOption
              query <- queryJson.as[JSONQuery].right.toOption
            } yield query
          }.getOrElse(JSONQuery.sortByKeys(keys))

        lookupData <- db.run(Registry().actions(entity).find(filter))

      } yield {
        Some(JSONFieldLookup.fromData(entity, JSONFieldMap(value, text, field.name), lookupData))
      }
    }} match {
      case Some(a) => a
      case None => Future.successful(None)
    }

    val condition = for{
      fieldId <- field.conditionFieldId
      values <- field.conditionValues
      json <- Try(parse(values).right.get.as[Seq[Json]].right.get).toOption
    } yield ConditionalField(fieldId,json)


    for{
      look <- lookup
      //      lab <- label
      //      placeHolder <- placeholder
      //      tip <- tooltip
    } yield {
      JSONField(
        field.`type`,
        field.name,
        false,
        false,
        fieldI18n.flatMap(_.label),
        None,
        look,
        fieldI18n.flatMap(_.placeholder),
        field.widget,
        None,
        field.default,
        None,
        condition
        //      fieldI18n.flatMap(_.tooltip)
      )
    }

  }

}
