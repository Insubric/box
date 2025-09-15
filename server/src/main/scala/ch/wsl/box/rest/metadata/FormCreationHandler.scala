package ch.wsl.box.rest.metadata

import ch.wsl.box.model.InformationSchema
import ch.wsl.box.model.boxentities.{BoxField, BoxForm, BoxUITable}
import ch.wsl.box.model.boxentities.BoxField.BoxField_row
import ch.wsl.box.model.shared.{JSONFieldTypes, Layout, LayoutBlock, WidgetsNames}
import ch.wsl.box.model.shared.admin.{ChildForm, FormCreationRequest}
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import slick.dbio.DBIO

import java.util.UUID
import scala.concurrent.ExecutionContext
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxForm.BoxForm_row
import io.circe.Json
import io.circe.syntax.EncoderOps


object FormCreationHandler {

  private case class NamedID(id:UUID,name:String)

  private def extractLayout(form:BoxForm_row):Layout = form.layout.flatMap(_.as[Layout].toOption).getOrElse(Layout(Seq()))

  private def addChildsToForm(parent:NamedID,childs:Seq[NamedID])(implicit executionContext: ExecutionContext):DBIO[Boolean] = {
    def addChildToForm(child:NamedID) = {
      for{
        fk <- InformationSchema.table(child.name).findFkToTable(parent.name)
        name = s"child_${child.name}"
        field = BoxField_row(
          form_uuid = parent.id,
          `type` = JSONFieldTypes.CHILD,
          name = name,
          widget = Some(WidgetsNames.simpleChild),
          child_form_uuid = Some(child.id),
          local_key_columns = fk.map(_.referencingKeys.toList),
          foreign_key_columns = fk.map(_.keys.toList)
        )
        field_row <- (BoxField.BoxFieldTable.returning(BoxField.BoxFieldTable) += field)
        keys = fk.toSeq.flatMap(_.referencingKeys)
        _ <- BoxField.BoxFieldTable.filter(_.form_uuid === child.id).filter(_.name inSet keys ).map(_.widget).update(Some(WidgetsNames.hidden))
      } yield NamedID(field_row.field_uuid.get,name)
    }

    import Layout._


    def updateLayout(l:Layout,fields:Seq[NamedID]):Layout = {
      val width = if(fields.size > 1) 4 else 6
      l.copy(
        blocks = l.blocks.map(_.copy(width = width)) ++ fields.map(x => LayoutBlock.simple(width,Seq(x.name)))
      )
    }

    for{
      fields <- DBIO.sequence(childs.map(addChildToForm))
      form <- BoxForm.BoxFormTable.filter(_.form_uuid === parent.id).result
      oldLayout = extractLayout(form.head)
      newLayout = updateLayout(oldLayout,fields)
      _ <- BoxForm.BoxFormTable.filter(_.form_uuid === parent.id).map(_.layout).update(Some(newLayout.asJson))
    } yield true


  }

  private def addChilds(name: String, parent:NamedID, childs: Seq[ChildForm])(implicit up:UserProfile, ec:ExecutionContext,services:Services):DBIO[Boolean] = {

    def addChild(child:ChildForm) = {
      val child_name = s"${name}_${child.entity}"
      for {
        id <- StubMetadataFactory.forEntity(child.entity,child_name)
        formEntity = NamedID(id,child.entity)
        _ <- addChilds(child_name,formEntity,child.childs)
      } yield formEntity
    }

    for{
      childs <- DBIO.sequence(childs.map(addChild))
      _ <- addChildsToForm(parent,childs)
    } yield true


  }


  private def addFormToHome(new_form:NamedID,roles:Seq[String])(implicit executionContext: ExecutionContext):DBIO[Boolean] = {

    def addToPage(page:BoxForm_row):DBIO[Boolean] = {
      val field = BoxField_row(
        form_uuid = page.form_uuid.get,
        `type` = JSONFieldTypes.CHILD,
        name = new_form.name,
        widget = Some(WidgetsNames.linkedForm),
        child_form_uuid = Some(new_form.id),
        params = Some(Json.obj("style" -> Json.fromString("box"))),
        roles = if(roles.nonEmpty) Some(roles.toList) else None
      )

      val oldLayout = extractLayout(page)

      val lastBlock = oldLayout.blocks.lastOption match {
        case Some(value) => value.copy(fields = value.fields ++ Seq(Left(new_form.name)))
        case None => LayoutBlock.simple(12,Seq(new_form.name))
      }

      val newLayout = oldLayout.copy(blocks = oldLayout.blocks.dropRight(1) ++ Seq(lastBlock))

      for{
        field_row <- (BoxField.BoxFieldTable.returning(BoxField.BoxFieldTable) += field)
        _ <- BoxForm.BoxFormTable.filter(_.form_uuid === page.form_uuid.get).map(_.layout).update(Some(newLayout.asJson))
      } yield true
    }

    for{
      conf <- BoxUITable.BoxUITable.filter(_.key === "index.page").result
      pages <- BoxForm.BoxFormTable.filter(_.name inSet conf.map(_.value)).result
      _ <- DBIO.sequence(pages.map(addToPage))
    } yield true
  }


  def apply(request:FormCreationRequest)(implicit up:UserProfile, ec:ExecutionContext,services:Services):DBIO[UUID] = {

    def addToHome(form_uuid:UUID):DBIO[Boolean] = request.add_to_home match {
      case true => addFormToHome(NamedID(form_uuid,request.name),request.roles)
      case false => DBIO.successful(true)
    }

    for{
      main_id <- StubMetadataFactory.forEntity(request.main_entity,request.name)
      _ <- addChilds(request.name,NamedID(main_id,request.main_entity),request.childs)
      _ <- addToHome(main_id)
    } yield main_id
  }
}
