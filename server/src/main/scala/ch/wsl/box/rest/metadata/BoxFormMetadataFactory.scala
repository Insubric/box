package ch.wsl.box.rest.metadata

import java.util.UUID
import akka.stream.Materializer
import ch.wsl.box.model.boxentities.{BoxForm, BoxFunction, BoxUser}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.routes.{Table, View}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

case class BoxFormMetadataFactory(implicit mat:Materializer, ec:ExecutionContext, services:Services) extends Logging with MetadataFactory {



  import ch.wsl.box.jdbc.PostgresProfile.api._


  import ch.wsl.box.rest.metadata.box.Constants._
  import ch.wsl.box.rest.metadata.box._


  val viewsOnly = Registry().fields.views.sorted
  val tablesAndViews = (viewsOnly ++ Registry().fields.tables).sorted



  def registry = for{
    forms <- getForms()
    users <- getUsers()
    functions <- getFunctions()
  } yield Seq(
    FormUIDef.main(tablesAndViews,users.sortBy(_.username)),
    FormUIDef.page(users.sortBy(_.username)),
    FormUIDef.field(tablesAndViews),
    FormUIDef.field_no_db(tablesAndViews),
    FormUIDef.field_childs(forms.sortBy(_.name)),
    FormUIDef.field_static(tablesAndViews,functions.map(_.name)),
    FormUIDef.fieldI18n(services.config.langs),
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

  override def list: DBIO[Seq[String]] = registry.map(_.filter(f => visibleAdmin.contains(f.objId)).map(_.name))

  override def of(name: String, lang: String): DBIO[JSONMetadata] = registry.map(_.find(_.name == name).get)

  override def of(id: UUID, lang: String): DBIO[JSONMetadata] = registry.map(_.find(_.objId == id).get)

  override def children(form: JSONMetadata): DBIO[Seq[JSONMetadata]] = for{
    forms <- getForms()
    functions <- getFunctions()
  } yield {
    form match {
      case f if f.objId == FORM => Seq(
        FormUIDef.field(tablesAndViews),
        FormUIDef.field_no_db(tablesAndViews),
        FormUIDef.field_static(tablesAndViews,functions.map(_.name)),
        FormUIDef.field_childs(forms),
        FormUIDef.fieldI18n(services.config.langs),
        FormUIDef.formI18n(viewsOnly,services.config.langs),
        FormUIDef.form_actions(functions.map(_.name)),
        FormUIDef.form_navigation_actions(functions.map(_.name))
      )
      case f if f.objId == PAGE => Seq(FormUIDef.field_static(tablesAndViews,functions.map(_.name)),FormUIDef.field_childs(forms),FormUIDef.fieldI18n(services.config.langs),FormUIDef.formI18n(viewsOnly,services.config.langs))
      case f if f.objId == FORM_FIELD => Seq(FormUIDef.fieldI18n(services.config.langs))
      case f if f.objId == FORM_FIELD_NOT_DB => Seq(FormUIDef.fieldI18n(services.config.langs))
      case f if f.objId == FORM_FIELD_STATIC => Seq(FormUIDef.fieldI18n(services.config.langs))
      case f if f.objId == FORM_FIELD_CHILDS => Seq(FormUIDef.fieldI18n(services.config.langs))
      case f if f.objId == FUNCTION => Seq(FunctionUIDef.field(tablesAndViews),FunctionUIDef.fieldI18n(services.config.langs),FunctionUIDef.functionI18n(services.config.langs))
      case f if f.objId == FUNCTION_FIELD => Seq(FunctionUIDef.fieldI18n(services.config.langs))
      case f if f.objId == NEWS => Seq(NewsUIDef.newsI18n(services.config.langs))
      case f if f.objId == LABEL_CONTAINER => Seq(LabelUIDef.label(services.config.langs))
      case _ => Seq()
    }
  }

}
