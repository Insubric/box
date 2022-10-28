package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.boxentities.BoxExportField.{BoxExportField_i18n_row, BoxExportField_row}
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

class ExportMetadataFactory(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,services:Services) extends Logging with DataMetadataFactory {

  import io.circe.generic.auto._

  implicit val db = up.db
  implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

  def list: Future[Seq[String]] = services.connection.adminDB.run{
    BoxExport.BoxExportTable.result
  }.map{_.sortBy(_.order.getOrElse(Double.MaxValue)).map(_.name)}

  def list(lang:String): Future[Seq[ExportDef]] = {

//    def accessibleExport = for {
//      roles <- up.memberOf
//      ex <- Export.Export.filter(ex => ex.access_role.isEmpty || ex.access_role inSet roles || roles.contains(up.name))
//    } yield ex

    def checkRole(roles:List[String], access_roles:List[String], accessLevel:Int) =  roles.intersect(access_roles).size>0 || access_roles.isEmpty || access_roles.contains(up.name) || accessLevel == 1000

    def query    = for {
       (e, ei18) <- BoxExport.BoxExportTable joinLeft(BoxExport.BoxExport_i18nTable.filter(_.lang === lang)) on(_.export_uuid === _.export_uuid)

    } yield (ei18.flatMap(_.label), e.function, e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip), e.access_role)

//    def queryResult = Auth.boxDB.run(query.result)


    for{
      roles <- up.memberOf
      al <- up.accessLevel
      qr <-  services.connection.adminDB.run(query.result)
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
      (e, ei18) <- BoxExport.BoxExportTable joinLeft(BoxExport.BoxExport_i18nTable.filter(_.lang === lang)) on(_.export_uuid === _.export_uuid)
      if e.function === name

    } yield (ei18.flatMap(_.label), e.function, e.name, e.order, ei18.flatMap(_.hint), ei18.flatMap(_.tooltip))

    services.connection.adminDB.run{
      query.result
    }.map(_.map{ case (label, function, name, _, hint, tooltip) =>
      ExportDef(function, label.getOrElse(name), hint, tooltip,FunctionKind.Modes.TABLE)
    }.head)
  }

  def of(schema:String, name:String, lang:String):Future[JSONMetadata]  = {
    val queryExport = for{
      (export, exportI18n) <- BoxExport.BoxExportTable joinLeft BoxExport.BoxExport_i18nTable.filter(_.lang === lang) on (_.export_uuid === _.export_uuid)
      if export.function === name

    } yield (export,exportI18n)

    def queryField(exportId:java.util.UUID) = for{
      (f, fi18n) <- BoxExportField.BoxExportFieldTable joinLeft BoxExportField.BoxExportField_i18nTable.filter(_.lang === lang) on (_.field_uuid === _.field_uuid)
                if f.export_uuid === exportId
    } yield (f, fi18n)

    for {
      (export,exportI18n) <- services.connection.adminDB.run {
        queryExport.result
      }.map(_.head)

      fields <- services.connection.adminDB.run {
        queryField(export.export_uuid.get).sortBy(_._1.field_uuid).result
      }

      jsonFields = fields.map(fieldsMetadata(schema,lang))

    } yield {

      if(exportI18n.isEmpty) logger.warn(s"Export ${export.name} (export_id: ${export.export_uuid}) has no translation to $lang")


//      val jsonFields = fields.map(fieldsMetadata(lang))

//      def defaultLayout:Layout = { // for subform default with 12
//        val default = Layout.fromFields(jsonFields)
//        default.copy(blocks = default.blocks.map(_.copy(width = 12)))
//      }

      val layout = Layout.fromString(export.layout).getOrElse(Layout.fromFields(jsonFields))

      val parameters = export.parameters.toSeq.flatMap(_.split(",").map(_.trim))

      JSONMetadata(
        export.export_uuid.get,
        export.function,
        EntityKind.EXPORT.kind,
        exportI18n.flatMap(_.label).getOrElse(name),
        jsonFields,layout,exportI18n.flatMap(_.function).getOrElse(export.function),
        lang,
        parameters,
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

  private def fieldsMetadata(schema:String, lang:String)(el:(BoxExportField_row, Option[BoxExportField_i18n_row])):JSONField = {
    import ch.wsl.box.shared.utils.JSONUtils._

    val (field,fieldI18n) = el

    if(fieldI18n.isEmpty) logger.warn(s"Export field ${field.name} (export_id: ${field.field_uuid}) has no translation to $lang")


    val lookup: Option[JSONFieldLookup] = for{
      entity <- field.lookupEntity
      value <- field.lookupValueField
      text <- fieldI18n.flatMap(_.lookupTextField)
    } yield JSONFieldLookup.fromData(entity, JSONFieldMap(value, text, field.name))


    val condition = for{
      fieldId <- field.conditionFieldId
      values <- field.conditionValues
      json <- Try(parse(values).right.get.as[Json].right.get).toOption
    } yield ConditionalField(fieldId,json)



      JSONField(
        field.`type`,
        field.name,
        false,
        false,
        fieldI18n.flatMap(_.label),
        None,
        lookup,
        fieldI18n.flatMap(_.placeholder),
        field.widget,
        None,
        field.default,
        condition
        //      fieldI18n.flatMap(_.tooltip)
      )


  }

}
