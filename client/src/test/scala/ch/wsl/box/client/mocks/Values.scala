package ch.wsl.box.client.mocks

import ch.wsl.box.model.shared.{Child, ConditionValue, ConditionalField, EntityKind, FormActionsMetadata, JSONField, JSONFieldTypes, JSONID, JSONKeyValue, JSONMetadata, Layout, LayoutBlock, NaturalKey, SurrugateKey, WidgetsNames}
import io.circe._
import io.circe.syntax._
import scribe.Level

import java.util.UUID

class Values(val loggerLevel:Level) {

  def loggedUser: Option[String] = Some("postgres")

  val headerLangEn = "test header en"
  val headerLangIt = "test header it"

  val titleId = "titleTest"
  val titleText = "TEST"

  val uiConf = Map(
    "title" -> "Test Title",
    "index.html" -> s"""<div id="$titleId">$titleText</div>""",
    "debug" -> "true"
  )

  val conf = Map(
    "langs" -> "it,en",
    "display.index.html" -> "true",
    "client.logger.level" -> loggerLevel.name
  )

  val testFormName = "test_form"
  val testFormTitle = "test form"

  val stringField = "string_field"
  val stringField2 = "string_field2"
  val conditionerField = "test_conditioner"
  val conditionalField = "test_conditional"
  val conditionalValue = "active"

  val formEntities = Seq(testFormName)

  val readOnlyField = "read_only_test"
  val readOnlyValue = "read_only_test_value"

  val id1 = UUID.fromString("e4f47af2-28a0-4732-8bcc-107d430f4ea3")
  val id2 = UUID.fromString("c8fc2910-a111-46a9-bdf5-c289f2f3199f")
  val id3 = UUID.fromString("6c5c0149-2aca-4a8f-b2bb-ede770529989")

  def metadata = JSONMetadata(
    id1,
    testFormName,
    EntityKind.FORM.kind,
    testFormTitle,
    fields = Seq(
      JSONField(
        JSONFieldTypes.NUMBER,
        "id",
        false
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = readOnlyField,
        nullable = true,
        readOnly = true
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = conditionerField,
        nullable = true
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = stringField,
        nullable = true
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = stringField2,
        nullable = true
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = conditionalField,
        nullable = true,
        condition = Some(ConditionalField(conditionerField,ConditionValue(conditionalValue.asJson)))
      ),
      JSONField(
        JSONFieldTypes.CHILD,
        name = "child",
        widget = Some(WidgetsNames.tableChild),
        nullable = false,
        child = Some(Child(
          objId = id2,
          key = "child",
          parent = Seq("id"),
          child = Seq("parent_id"),
          childQuery = None,
          props = "",
          hasData = true
        ))
      )
    ),
    layout = Layout(Seq(LayoutBlock(None,Seq(
      Left("child"),
      Left(readOnlyField),
      Left(conditionerField),
      Left(conditionalField),
      Left(stringField),
      Left(stringField2),
    ),12))),
    entity = "test",
    lang = "it",
    tabularFields = Seq("id"),
    rawTabularFields = Seq("id"),
    keys = Seq("id"),
    query = None,
    exportFields = Seq("id"),
    view = None,
    action = FormActionsMetadata.default,
    keyStrategy = SurrugateKey,
    static = false
  )

  def childMetadata = JSONMetadata(
    id2,
    "child",
    EntityKind.FORM.kind,
    "Child form",
    fields = Seq(
      JSONField(
        JSONFieldTypes.NUMBER,
        "id",
        false
      ),
      JSONField(
        JSONFieldTypes.NUMBER,
        name = "parent_id",
        nullable = true
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = "text",
        nullable = true
      ),
      JSONField(
        JSONFieldTypes.CHILD,
        name = "subchild",
        widget = Some(WidgetsNames.tableChild),
        nullable = false,
        child = Some(Child(
          objId = id3,
          key = "subchild",
          parent = Seq("id"),
          child = Seq("child_id"),
          childQuery = None,
          props = "",
          hasData = true
        ))
      )
    ),
    layout = Layout(Seq(LayoutBlock(None,Seq(Left("id"),Left("parent_id"),Left("text"),Left("subchild")),12))),
    entity = "test_child",
    lang = "it",
    tabularFields = Seq("id"),
    rawTabularFields = Seq("id"),
    keys = Seq("id"),
    query = None,
    exportFields = Seq("id"),
    view = None,
    action = FormActionsMetadata.default,
    keyStrategy = SurrugateKey,
  )

  val subchildMetadata = JSONMetadata(
    id3,
    "subchild",
    EntityKind.FORM.kind,
    "SubChild form",
    fields = Seq(
      JSONField(
        JSONFieldTypes.NUMBER,
        "id",
        false
      ),
      JSONField(
        JSONFieldTypes.NUMBER,
        name = "child_id",
        nullable = false
      ),
      JSONField(
        JSONFieldTypes.STRING,
        name = "text_subchild",
        nullable = true
      )
    ),
    layout = Layout(Seq(LayoutBlock(None,Seq(Left("id"),Left("child_id"),Left("text_subchild")),12))),
    entity = "test_subchild",
    lang = "it",
    tabularFields = Seq("id"),
    rawTabularFields = Seq("id"),
    keys = Seq("id"),
    query = None,
    exportFields = Seq("id"),
    view = None,
    action = FormActionsMetadata.default,
    keyStrategy = SurrugateKey,
  )

  object ids {
    object main {
      val singleChild: JSONID = JSONID(Vector(JSONKeyValue("id", Json.fromInt(1))))
      val doubleChild: JSONID = JSONID(Vector(JSONKeyValue("id",  Json.fromInt(2))))
    }
    object childs {
      val thirdChild: JSONID = JSONID(Vector(JSONKeyValue("id",  Json.fromInt(3))))
    }

  }

  def get(id:JSONID):Json = {
    id match {
      case ids.main.singleChild => Map(
        "id" -> 1.asJson,
        readOnlyField -> readOnlyValue.asJson,
        "child" -> Seq(
          Map("parent_id" -> 1.asJson, "id" -> 1.asJson, "text" -> "test".asJson)
        ).asJson
      ).asJson
      case ids.main.doubleChild => Map(
        "id" -> 2.asJson,
        "child" -> Seq(
          Map("parent_id" -> 2.asJson, "id" -> 2.asJson, "text" -> "test".asJson),
          Map("parent_id" -> 2.asJson, "id" -> 3.asJson, "text" -> "test".asJson)
        ).asJson
      ).asJson
    }
  }

  def insert(data:Json):Json = {
    data
  }

  def update(id:JSONID,obj:Json):Json = {
    println("not implemented")
    ???
  }

  def children(entity:String):Seq[JSONMetadata] = {
    Seq(childMetadata,subchildMetadata)
  }

}
