package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared._

object FunctionUIDef {

  import io.circe._
  import io.circe.syntax._
  import Constants._

  val main = JSONMetadata(
    objId = FUNCTION,
    kind = EntityKind.BOX_FORM.kind,
    name = "function",
    label = "Function builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"function_uuid",false),
      JSONField(JSONFieldTypes.STRING,"name",false),
      JSONField(JSONFieldTypes.STRING,"function",false, widget = Some(WidgetsNames.code),label = Some(""),
        params = Some(Json.obj("language" -> "java".asJson, "height" -> 800.asJson)),
        default = Some(
        """
          |/*
          | * Must return a Future[DataResult] that represent a CSV table
          | * `context` is provided that has:
          | * - data:io.circe.Json that store the data of the parameters
          | * - ws:ch.wsl.box.rest.logic.RuntimeWS that exposes `get` and `post` methods to interact with external WS
          | * - psql:ch.wsl.box.rest.logic.RuntimePSQL that exposes `function` and `table` to call the underlying DB
          | */
          |
          |for{
          |  result <- Future.successful{
          |     DataResultTable(headers = Seq("demo"), headerType = Seq("string"), rows = Seq(Seq(Json.fromString("demo"))))
          |  }
          |} yield result
          |
        """.stripMargin),
      ),
      JSONField(JSONFieldTypes.STRING,"presenter",true, widget = Some(WidgetsNames.code),label = Some(""),
        params = Some(Json.obj("language" -> "handlebars".asJson, "height" -> 800.asJson)),
      ),
      JSONField(JSONFieldTypes.STRING,"description",true),
      JSONField(JSONFieldTypes.STRING,"mode",false,widget = Some(WidgetsNames.select), lookup = Some(JSONFieldLookup.prefilled(
        FunctionKind.Modes.all.map(x => JSONLookup(x.asJson,Seq(x)))
      ))),
      JSONField(JSONFieldTypes.STRING,"layout",true, widget = Some(WidgetsNames.textarea),label = Some("")),
      JSONField(JSONFieldTypes.NUMBER,"order",true),
      JSONField(JSONFieldTypes.CHILD,"function_field",true,child = Some(Child(FUNCTION_FIELD,"function_field",Seq("function_uuid"),Seq("function_uuid"),None,"",true))),
      JSONField(JSONFieldTypes.CHILD,"function_i18n",true,child = Some(Child(FUNCTION_I18N,"function_i18n",Seq("function_uuid"),Seq("function_uuid"),None,"",true)))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,Seq(
          SubLayoutBlock(None,Some(Seq(5,1,6)),Seq(
            Right(
              SubLayoutBlock(Some(Left("Base Info")),Some(Seq(12)),Seq("function_uuid","name","order","description","mode").map(Left(_)))
            ),
            Left(""),
            Right(
              SubLayoutBlock(Some(Left("Layout")),Some(Seq(12)),Seq("layout").map(Left(_)))
            )
          )),
          SubLayoutBlock(Some(Left("Function")),Some(Seq(12)),Seq("function").map(Left(_))),
          SubLayoutBlock(Some(Left("Presenter")),Some(Seq(12)),Seq("presenter").map(Left(_))),
        ).map(Right(_)),8),
        LayoutBlock(Some(Left("I18n")),Seq("function_i18n").map(Left(_)),4),
        LayoutBlock(Some(Left("Fields")),Seq("function_field").map(Left(_)),12),
      )
    ),
    entity = "function",
    lang = "en",
    tabularFields = Seq("function_uuid","name","description"),
    rawTabularFields = Seq("function_uuid","name","description"),
    keys = Seq("function_uuid"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field(tables:Seq[String]) = JSONMetadata(
    objId = FUNCTION_FIELD,
    kind = EntityKind.BOX_FORM.kind,
    name = "field",
    label = "Field builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"function_uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.name,
      CommonField.widget,
      CommonField.typ(child = false),
      JSONField(JSONFieldTypes.CHILD,"function_field_i18n",true,child = Some(Child(FUNCTION_FIELD_I18N,"function_field_i18n",Seq("field_uuid"),Seq("field_uuid"),None,"widget",true))),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      CommonField.lookupQuery(tables),
      CommonField.default,
      CommonField.condition,
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock.simple(4,Seq(
          "field_uuid",
          "function_uuid",
          "name",
          "type",
          "widget",
          "lookupEntity",
          "lookupValueField",
          "lookupQuery",
          "default",
          "condition"
        )),
        LayoutBlock.simple(8,Seq("function_field_i18n")),
      )
    ),
    entity = "function_field",
    lang = "en",
    tabularFields = Seq("field_uuid","function_uuid","name","widget"),
    rawTabularFields = Seq("field_uuid","function_uuid","name","widget"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def fieldI18n(langs:Seq[String]) = JSONMetadata(
    objId = FUNCTION_FIELD_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "fieldI18n",
    label = "FieldI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.lang(langs),
      CommonField.simpleLabel,
      CommonField.tooltip,
      CommonField.hint,
      CommonField.placeholder(Seq(WidgetsNames.input)),
      CommonField.lookupTextField(Seq(WidgetsNames.select,WidgetsNames.popup)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock.simple(3,Seq("field_uuid","uuid","lang")),
        LayoutBlock.simple(9,Seq("label","placeholder","tooltip","hint","lookupTextField")),
      )
    ),
    entity = "function_field_i18n",
    lang = "en",
    tabularFields = Seq("field_uuid","uuid","lang","label"),
    rawTabularFields = Seq("field_uuid","uuid","lang","label"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def functionI18n(langs:Seq[String]) = JSONMetadata(
    objId = FUNCTION_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "FormI18n builder",
    label = "FormI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"function_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.lang(langs),
      CommonField.simpleLabel,
      CommonField.tooltip,
      CommonField.hint,
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock.simple(12,Seq("lang","label","tooltip","hint"))
      )
    ),
    entity = "function_i18n",
    lang = "en",
    tabularFields = Seq("function_uuid","uuid","lang","label"),
    rawTabularFields = Seq("function_uuid","uuid","lang","label"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )



}
