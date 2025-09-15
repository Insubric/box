package ch.wsl.box.rest.metadata

import java.util.UUID
import akka.stream.Materializer
import ch.wsl.box.model.InformationSchema
import ch.wsl.box.model.boxentities._
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

object BoxFormMetadataFactory extends Logging with MetadataFactory {


  import ch.wsl.box.jdbc.PostgresProfile.api._


  import ch.wsl.box.rest.metadata.box.Constants._
  import ch.wsl.box.rest.metadata.box._


  lazy val viewsOnly = Registry().fields.views.sorted
  lazy val tablesAndViews = (viewsOnly ++ Registry().fields.tables).sorted

  lazy val fields: Map[String, Seq[String]] = Registry().fields.tableFields.view.mapValues(_.keys.toSeq).toMap



  def registry(implicit ec:ExecutionContext,services:Services) = for{
    forms <- getForms()
    users <- getUsers()
    functions <- getFunctions()
    roles <- InformationSchema.roles()
  } yield Seq(
    FormUIDef.main(tablesAndViews,users.sortBy(_.username),fields),
    FormUIDef.page(users.sortBy(_.username)),
    FormUIDef.field(tablesAndViews,fields,roles),
    FormUIDef.field_no_db(tablesAndViews,fields),
    FormUIDef.field_childs(forms.sortBy(_.name),fields,roles),
    FormUIDef.field_static(tablesAndViews,functions.map(_.name),fields,roles),
    FormUIDef.fieldI18n(services.config.langs,fields),
    FormUIDef.formI18n(viewsOnly,services.config.langs),
    FormUIDef.form_actions(functions.map(_.name)),
    FormUIDef.form_navigation_actions(functions.map(_.name)),
    FunctionUIDef.main,
    FunctionUIDef.field(tablesAndViews),
    FunctionUIDef.fieldI18n(services.config.langs),
    FunctionUIDef.functionI18n(services.config.langs),
    NewsUIDef.main,
    NewsUIDef.newsI18n(services.config.langs),
    LabelUIDef.label(services.config.langs),
    LabelUIDef.labelContainer
  )

  def getForms():DBIO[Seq[BoxForm.BoxForm_row]] = {
      BoxForm.BoxFormTable.result
  }

  def getFunctions():DBIO[Seq[BoxFunction.BoxFunction_row]] = {
    BoxFunction.BoxFunctionTable.result
  }

  def getUsers():DBIO[Seq[BoxUser.BoxUser_row]] = {
      BoxUser.BoxUserTable.result
  }

  def fieldTypes = Registry().fields.tableFields.mapValues(_.mapValues{col =>
    col.jsonType
  })

  val visibleAdmin = Seq(FUNCTION,FORM,PAGE,NEWS,LABEL)

  override def list(implicit ec:ExecutionContext,services:Services): DBIO[Seq[String]] = registry.map(_.filter(f => visibleAdmin.contains(f.objId)).map(_.name))

  override def of(name: String, lang: String,user:CurrentUser)(implicit ec:ExecutionContext,services:Services): DBIO[JSONMetadata] = registry.map(_.find(_.name == name).get)

  override def of(id: UUID, lang: String,user:CurrentUser)(implicit ec:ExecutionContext,services:Services): DBIO[JSONMetadata] = registry.map(_.find(_.objId == id).get)

  override def children(form: JSONMetadata,user:CurrentUser,ignoreChilds:Seq[UUID] = Seq())(implicit ec:ExecutionContext,services:Services): DBIO[Seq[JSONMetadata]] = for{
    forms <- getForms()
    functions <- getFunctions()
    roles <- InformationSchema.roles()
  } yield {
    form match {
      case f if f.objId == FORM => Seq(
        FormUIDef.field(tablesAndViews,fields,roles),
        FormUIDef.field_no_db(tablesAndViews,fields),
        FormUIDef.field_static(tablesAndViews,functions.map(_.name),fields,roles),
        FormUIDef.field_childs(forms,fields,roles),
        FormUIDef.fieldI18n(services.config.langs,fields),
        FormUIDef.formI18n(viewsOnly,services.config.langs),
        FormUIDef.form_actions(functions.map(_.name)),
        FormUIDef.form_navigation_actions(functions.map(_.name))
      )
      case f if f.objId == PAGE => Seq(FormUIDef.field_static(tablesAndViews,functions.map(_.name),fields,roles),FormUIDef.field_childs(forms,fields,roles),FormUIDef.fieldI18n(services.config.langs,fields),FormUIDef.formI18n(viewsOnly,services.config.langs))
      case f if f.objId == FORM_FIELD => Seq(FormUIDef.fieldI18n(services.config.langs,fields))
      case f if f.objId == FORM_FIELD_NOT_DB => Seq(FormUIDef.fieldI18n(services.config.langs,fields))
      case f if f.objId == FORM_FIELD_STATIC => Seq(FormUIDef.fieldI18n(services.config.langs,fields))
      case f if f.objId == FORM_FIELD_CHILDS => Seq(FormUIDef.fieldI18n(services.config.langs,fields))
      case f if f.objId == FUNCTION => Seq(FunctionUIDef.field(tablesAndViews),FunctionUIDef.fieldI18n(services.config.langs),FunctionUIDef.functionI18n(services.config.langs))
      case f if f.objId == FUNCTION_FIELD => Seq(FunctionUIDef.fieldI18n(services.config.langs))
      case f if f.objId == NEWS => Seq(NewsUIDef.newsI18n(services.config.langs))
      case f if f.objId == LABEL_CONTAINER => Seq(LabelUIDef.label(services.config.langs))
      case _ => Seq()
    }
  }

}
