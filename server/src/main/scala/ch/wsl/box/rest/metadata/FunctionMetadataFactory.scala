package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.model.EntityActionsRegistry
import ch.wsl.box.model.boxentities.ExportField.{ExportField_i18n_row, ExportField_row}
import ch.wsl.box.model.boxentities.Function.{FunctionField_i18n_row, FunctionField_row}
import ch.wsl.box.model.boxentities.{Export, ExportField}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.utils.{Auth, UserProfile}
import io.circe.Json
import io.circe.parser.parse
import scribe.Logging
import ch.wsl.box.rest.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class FunctionMetadataFactory(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext) extends Logging with DataMetadataFactory {

  import io.circe.generic.auto._

  implicit val db = up.db

  def functions = ch.wsl.box.model.boxentities.Function

  def list: Future[Seq[String]] = Auth.boxDB.run{
    functions.Function.result
  }.map{_.sortBy(_.order.getOrElse(Double.MaxValue)).map(_.name)}

  def list(lang:String): Future[Seq[ExportDef]] = {

    //    def accessibleExport = for {
    //      roles <- up.memberOf
    //      ex <- Export.Export.filter(ex => ex.access_role.isEmpty || ex.access_role inSet roles || roles.contains(up.name))
    //    } yield ex

    def checkRole(roles:List[String], access_roles:List[String], accessLevel:Int) =  roles.intersect(access_roles).size>0 || access_roles.isEmpty || access_roles.contains(up.name) || accessLevel == 1000

    def query    = for {
      (e, ei18) <- functions.Function joinLeft(functions.Function_i18n.filter(_.lang === lang)) on(_.function_id === _.function_id)

    } yield (ei18.flatMap(_.label), e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip), e.access_role, e.mode)

    //    def queryResult = Auth.boxDB.run(query.result)


    for{
      al <- up.accessLevel
      qr <-  Auth.boxDB.run(query.result)
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
      (e, ei18) <- functions.Function joinLeft(functions.Function_i18n.filter(_.lang === lang)) on(_.function_id === _.function_id)
      if e.name === name

    } yield (ei18.flatMap(_.label), e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip),e.mode)

    Auth.boxDB.run{
      query.result
    }.map(_.map{ case (label, name, _, hint, tooltip, mode) =>
      ExportDef(name, label.getOrElse(name), hint, tooltip, mode)
    }.head)
  }

  def of(name:String, lang:String):Future[JSONMetadata]  = {
    val queryExport = for{
      (func, functionI18n) <- functions.Function joinLeft functions.Function_i18n.filter(_.lang === lang) on (_.function_id === _.function_id)
      if func.name === name

    } yield (func,functionI18n)

    def queryField(functionId:Int) = for{
      (f, fi18n) <- functions.FunctionField joinLeft functions.FunctionField_i18n.filter(_.lang === lang) on (_.field_id === _.field_id)
      if f.function_id === functionId
    } yield (f, fi18n)

    for {
      (func, functionI18n)  <- Auth.boxDB.run {
        queryExport.result
      }.map(_.head)

      fields <- Auth.boxDB.run {
        queryField(func.function_id.get).sortBy(_._1.field_id).result
      }

      jsonFields <- Future.sequence(fields.map(fieldsMetadata(lang)))

    } yield {

      if(functionI18n.isEmpty) logger.warn(s"Export ${func.name} (function_id: ${func.function_id}) has no translation to $lang")


      val layout = Layout.fromString(func.layout).getOrElse(Layout.fromFields(jsonFields))


      JSONMetadata(func.function_id.get,func.name,functionI18n.flatMap(_.label).getOrElse(name),jsonFields,layout,"function",lang,Seq(),Seq(),None,Seq())//,"")
    }
  }

  private def fieldsMetadata(lang:String)(el:(FunctionField_row, Option[FunctionField_i18n_row])):Future[JSONField] = {
    import ch.wsl.box.shared.utils.JSONUtils._

    val (field,fieldI18n) = el

    if(fieldI18n.isEmpty) logger.warn(s"Export field ${field.name} (export_id: ${field.field_id}) has no translation to $lang")


    val lookup: Future[Option[JSONFieldLookup]] = {for{
      entity <- field.lookupEntity
      value <- field.lookupValueField
      text <- fieldI18n.flatMap(_.lookupTextField)

    } yield {
      import io.circe.generic.auto._
      for {

        keys <- EntityMetadataFactory.keysOf(entity)
        filter = {
            for{
              queryString <- field.lookupQuery
              queryJson <- parse(queryString).right.toOption
              query <- queryJson.as[JSONQuery].right.toOption
            } yield query
          }.getOrElse(JSONQuery.sortByKeys(keys))

        lookupData <- EntityActionsRegistry().tableActions(entity).find(filter)

      } yield {
        Some(JSONFieldLookup.fromData(entity, JSONFieldMap(value, text), lookupData))
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
        fieldI18n.flatMap(_.label),
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
