package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.model.boxentities.BoxExportField.{BoxExportField_i18n_row, BoxExportField_row}
import ch.wsl.box.model.boxentities.{BoxExport, BoxExportField}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.utils.{Auth, UserProfile}
import io.circe.Json
import io.circe.parser.parse
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class ExportMetadataFactory(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext) extends Logging with DataMetadataFactory {

  import io.circe.generic.auto._

  implicit val db = up.db

  def list: Future[Seq[String]] = Auth.boxDB.run{
    BoxExport.BoxExportTable.result
  }.map{_.sortBy(_.order.getOrElse(Double.MaxValue)).map(_.name)}

  def list(lang:String): Future[Seq[ExportDef]] = {

//    def accessibleExport = for {
//      roles <- up.memberOf
//      ex <- Export.Export.filter(ex => ex.access_role.isEmpty || ex.access_role inSet roles || roles.contains(up.name))
//    } yield ex

    def checkRole(roles:List[String], access_roles:List[String], accessLevel:Int) =  roles.intersect(access_roles).size>0 || access_roles.isEmpty || access_roles.contains(up.name) || accessLevel == 1000

    def query    = for {
       (e, ei18) <- BoxExport.BoxExportTable joinLeft(BoxExport.BoxExport_i18nTable.filter(_.lang === lang)) on(_.export_id === _.export_id)

    } yield (ei18.flatMap(_.label), e.function, e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip), e.access_role)

//    def queryResult = Auth.boxDB.run(query.result)


    for{
      roles <- up.memberOf
      al <- up.accessLevel
      qr <-  Auth.boxDB.run(query.result)
    } yield {
       qr.filter(_._7.map(ar => checkRole(roles, ar, al)).getOrElse(true))
         .sortBy(_._4.getOrElse(Double.MaxValue)).map(
         { case (label, function, name, _, hint, tooltip, _) =>
           ExportDef(function, label.getOrElse(name), hint, tooltip,FunctionKind.Modes.TABLE)
         })

    }

  }

  def defOf(name:String, lang:String): Future[ExportDef] = {
    val query = for {
      (e, ei18) <- BoxExport.BoxExportTable joinLeft(BoxExport.BoxExport_i18nTable.filter(_.lang === lang)) on(_.export_id === _.export_id)
      if e.function === name

    } yield (ei18.flatMap(_.label), e.function, e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip))

    Auth.boxDB.run{
      query.result
    }.map(_.map{ case (label, function, name, _, hint, tooltip) =>
      ExportDef(function, label.getOrElse(name), hint, tooltip,FunctionKind.Modes.TABLE)
    }.head)
  }

  def of(name:String, lang:String):Future[JSONMetadata]  = {
    val queryExport = for{
      (export, exportI18n) <- BoxExport.BoxExportTable joinLeft BoxExport.BoxExport_i18nTable.filter(_.lang === lang) on (_.export_id === _.export_id)
      if export.function === name

    } yield (export,exportI18n)

    def queryField(exportId:Int) = for{
      (f, fi18n) <- BoxExportField.BoxExportFieldTable joinLeft BoxExportField.BoxExportField_i18nTable.filter(_.lang === lang) on (_.field_id === _.field_id)
                if f.export_id === exportId
    } yield (f, fi18n)

    for {
      (export,exportI18n) <- Auth.boxDB.run {
        queryExport.result
      }.map(_.head)

      fields <- Auth.boxDB.run {
        queryField(export.export_id.get).sortBy(_._1.field_id).result
      }

      jsonFields <- Future.sequence(fields.map(fieldsMetadata(lang)))

    } yield {

      if(exportI18n.isEmpty) logger.warn(s"Export ${export.name} (export_id: ${export.export_id}) has no translation to $lang")


//      val jsonFields = fields.map(fieldsMetadata(lang))

//      def defaultLayout:Layout = { // for subform default with 12
//        val default = Layout.fromFields(jsonFields)
//        default.copy(blocks = default.blocks.map(_.copy(width = 12)))
//      }

      val layout = Layout.fromString(export.layout).getOrElse(Layout.fromFields(jsonFields))

      val parameters = export.parameters.toSeq.flatMap(_.split(","))

      JSONMetadata(export.export_id.get,export.function,exportI18n.flatMap(_.label).getOrElse(name),jsonFields,layout,exportI18n.flatMap(_.function).getOrElse(export.function),lang,parameters,Seq(),Seq(),None,Seq(),None,FormActionsMetadata.default)//,"")
    }
  }

  private def fieldsMetadata(lang:String)(el:(BoxExportField_row, Option[BoxExportField_i18n_row])):Future[JSONField] = {
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
        filter = { for{
          queryString <- field.lookupQuery
          queryJson <- parse(queryString).right.toOption
          query <- queryJson.as[JSONQuery].right.toOption
        } yield query }.getOrElse(JSONQuery.sortByKeys(keys))

        lookupData <- db.run(Registry().actions.tableActions(ec)(entity).find(filter))

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
