package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared.{CurrentUser, EntityKind, JSONField, JSONID, JSONMetadata, JSONQuery, Layout, WidgetsNames}
import _root_.io.circe.Json
import _root_.io.circe.syntax._
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.rest.metadata.{FormMetadataFactory, MetadataFactory}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import ch.wsl.box.testmodel.Entities.{App_child, App_parent, App_parent_row}
import slick.dbio

import java.util.UUID
import scala.concurrent.ExecutionContext

class InsertTablularChildSpec extends BaseSpec {

  val idParent = "id"
  val idChild = "child_id"
  val textChild = "name"
  val insertedChildText = "text-test"

  val parentName = "app_parent"
  val childId = UUID.randomUUID()
  val parentId = UUID.randomUUID()
  val childName = "app_child"

  val metadata: JSONMetadata = JSONMetadata.simple(parentId,EntityKind.FORM.kind,parentName,"it",Seq(
    JSONField.number(idParent,nullable = false),
    JSONField.child(childName,childId,Seq(idParent),Seq("parent_id")).withWidget(WidgetsNames.editableTable).copy(params = Some(Json.fromFields(Map(
      "fields" -> Seq(idChild,textChild).asJson
    ))))
  ),Seq(idParent))


  val childMetadata: JSONMetadata = JSONMetadata.simple(childId,EntityKind.FORM.kind,childName,"it",Seq(
    JSONField.number(idChild,nullable = false),
    JSONField.number("parent_id",nullable = false),
    JSONField.string(textChild)
  ),Seq(idChild)).copy(layout = Layout(Seq()))

  val testMetadataFactory = new MetadataFactory{
    override def of(name: String, lang: String, user: CurrentUser)(implicit ec: ExecutionContext, services: Services): dbio.DBIO[JSONMetadata] = DBIO.successful{ name match {
      case n:String if parentName == n => metadata
      case _ => childMetadata
    }}

    override def of(id: UUID, lang: String, user: CurrentUser)(implicit ec: ExecutionContext, services: Services): dbio.DBIO[JSONMetadata] = DBIO.successful{ id match {
      case i:UUID if i == parentId => metadata
      case _ => childMetadata
    }}

    override def children(form: JSONMetadata, user: CurrentUser, ignoreChilds: Seq[UUID])(implicit ec: ExecutionContext, services: Services): dbio.DBIO[Seq[JSONMetadata]] = DBIO.successful(Seq(childMetadata))

    override def list(implicit ec: ExecutionContext, services: Services): dbio.DBIO[Seq[String]] = ???
  }

  "Editable table child"  should "inserted with all columns"  in withServices[Assertion] { implicit services =>
    implicit val session = BoxSession(CurrentUser.simple(services.connection.adminUser))
    implicit val up = UserProfile.simple(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    val actions = FormActions(metadata,Registry(),testMetadataFactory)

    val json = _root_.io.circe.parser.parse(
      s"""
        |{
        | "id": 1,
        | "$childName": [{
        |   "id": 2,
        |   "$textChild": "$insertedChildText"
        | }]
        |}
        |
        |""".stripMargin).toOption.get

    for{
      i <- up.db.run(actions.insert(json).transactionally)
      resultParent <- up.db.run(App_parent.result)
      result <- up.db.run(App_child.result)
    } yield {
      i.js(childName).asArray.isDefined shouldBe true
      i.js(childName).asArray.get.length shouldBe 1
      i.js(childName).asArray.get.head.get(textChild) shouldBe insertedChildText
      resultParent.length shouldBe 1
      result.length shouldBe 1
      result.head.name shouldBe Some(insertedChildText)
    }

  }


}

