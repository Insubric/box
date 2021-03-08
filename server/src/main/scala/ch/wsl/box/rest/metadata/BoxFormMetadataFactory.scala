package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.model.boxentities.{BoxForm, BoxUser}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.routes.{Table, View}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

case class BoxFormMetadataFactory(implicit mat:Materializer, ec:ExecutionContext) extends Logging with MetadataFactory {



  import ch.wsl.box.jdbc.PostgresProfile.api._


  import ch.wsl.box.rest.metadata.box.Constants._
  import ch.wsl.box.rest.metadata.box._


  val viewsOnly = Registry().fields.views.sorted
  val tablesAndViews = (viewsOnly ++ Registry().fields.tables).sorted



  def registry = for{
    forms <- getForms()
    users <- getUsers()
  } yield Seq(
    FormUIDef.main(tablesAndViews,users.sortBy(_.username)),
    FormUIDef.page,
    FormUIDef.field(tablesAndViews),
    FormUIDef.field_childs(forms),
    FormUIDef.field_static(tablesAndViews),
    FormUIDef.fieldI18n,
    FormUIDef.formI18n(viewsOnly),
    FormUIDef.fieldFile,
    FunctionUIDef.main,
    FunctionUIDef.field(tablesAndViews),
    FunctionUIDef.fieldI18n,
    FunctionUIDef.functionI18n,
    NewsUIDef.main,
    NewsUIDef.newsI18n,
    LabelUIDef.label,
    LabelUIDef.labelContainer
  )

  def getForms():DBIO[Seq[BoxForm.BoxForm_row]] = {
      BoxForm.BoxFormTable.result
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

  override def of(id: Int, lang: String): DBIO[JSONMetadata] = registry.map(_.find(_.objId == id).get)

  override def children(form: JSONMetadata): DBIO[Seq[JSONMetadata]] = getForms().map{ forms =>
    form match {
      case f if f.objId == FORM => Seq(FormUIDef.field(tablesAndViews),FormUIDef.field_static(tablesAndViews),FormUIDef.field_childs(forms),FormUIDef.fieldI18n,FormUIDef.formI18n(viewsOnly),FormUIDef.fieldFile)
      case f if f.objId == PAGE => Seq(FormUIDef.field_static(tablesAndViews),FormUIDef.field_childs(forms),FormUIDef.fieldI18n,FormUIDef.formI18n(viewsOnly),FormUIDef.fieldFile)
      case f if f.objId == FORM_FIELD => Seq(FormUIDef.fieldI18n,FormUIDef.fieldFile)
      case f if f.objId == FORM_FIELD_STATIC => Seq(FormUIDef.fieldI18n)
      case f if f.objId == FORM_FIELD_CHILDS => Seq(FormUIDef.fieldI18n)
      case f if f.objId == FUNCTION => Seq(FunctionUIDef.field(tablesAndViews),FunctionUIDef.fieldI18n,FunctionUIDef.functionI18n)
      case f if f.objId == FUNCTION_FIELD => Seq(FunctionUIDef.fieldI18n)
      case f if f.objId == NEWS => Seq(NewsUIDef.newsI18n)
      case f if f.objId == LABEL_CONTAINER => Seq(LabelUIDef.label)
      case _ => Seq()
    }
  }

}
