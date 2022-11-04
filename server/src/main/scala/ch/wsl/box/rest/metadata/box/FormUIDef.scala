package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.boxentities.BoxForm
import ch.wsl.box.model.boxentities.BoxUser.BoxUser_row
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.metadata.FormMetadataFactory

object FormUIDef {

  import io.circe._
  import io.circe.syntax._
  import Constants._

  def main(tables:Seq[String], users:Seq[BoxUser_row]) = JSONMetadata(
    objId = FORM,
    kind = EntityKind.BOX_FORM.kind,
    name = "form",
    label = "Form - Interface builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.inputDisabled)),
      CommonField.formName,
      CommonField.formDescription,
      CommonField.formLayout,
      CommonField.formProps,
      CommonField.params,
      JSONField(JSONFieldTypes.STRING,"entity",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          tables.map(x => JSONLookup(x.asJson,Seq(x)))
        ))
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"show_navigation",false,
        widget = Some(WidgetsNames.checkbox),
        default = Some("true")
      ),
      JSONField(JSONFieldTypes.STRING,"tabularFields",false,widget = Some(WidgetsNames.textarea)),
      JSONField(JSONFieldTypes.STRING,"query",true,
        widget = Some(WidgetsNames.code),
        params = Some(Json.obj("language" -> "json".asJson, "height" -> 100.asJson, "fullWidth" -> false.asJson))
      ),
      JSONField(JSONFieldTypes.STRING,"guest_user",true,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          users.map(x => JSONLookup(x.username.asJson,Seq(x.username)))
        ))
      ),
      JSONField(JSONFieldTypes.STRING,"edit_key_field",true,
        widget = Some(WidgetsNames.input),
        label = Some("Key fields"),
        placeholder = Some("by default primary key is used"),
        tooltip = Some("Manually enter the fields that should be used as primary key. This is useful mainly for updatable views where the primary key of the entity cannot be calculated. Fields are separated with comma")
      ),
      JSONField(JSONFieldTypes.STRING,"exportFields",true,widget = Some(WidgetsNames.textarea)),
      JSONField(JSONFieldTypes.CHILD,"fields",true,
        child = Some(Child(FORM_FIELD,"fields","form_uuid","form_uuid",
          Some(JSONQuery.sortByKeys(Seq("name")).filterWith(
            JSONQueryFilter("type",Some("notin"),JSONFieldTypes.STATIC+","+JSONFieldTypes.CHILD),
            JSONQueryFilter.WHERE.eq("entity_field","true")
          )),
          props = "entity"
        )),
        widget = Some(WidgetsNames.tableChild)
      ),
      JSONField(JSONFieldTypes.CHILD,"fields_no_db",true,
        child = Some(Child(FORM_FIELD_NOT_DB,"fields_no_db","form_uuid","form_uuid",
          Some(JSONQuery.sortByKeys(Seq("name")).filterWith(
            JSONQueryFilter("type",Some("notin"),JSONFieldTypes.STATIC+","+JSONFieldTypes.CHILD),
            JSONQueryFilter.WHERE.eq("entity_field","false")
          )),
          props = "entity"
        )),
        widget = Some(WidgetsNames.tableChild)
      ),
      JSONField(JSONFieldTypes.CHILD,"form_actions",true,
        child = Some(Child(FORM_ACTION,"form_actions","form_uuid","form_uuid", None,"")),
        widget = Some(WidgetsNames.tableChild),
        params = Some(Json.fromFields(Map("sortable" -> Json.True)))
      ),
      JSONField(JSONFieldTypes.CHILD,"form_navigation_actions",true,
        child = Some(Child(FORM_NAVIGATION_ACTION,"form_navigation_actions","form_uuid","form_uuid", None,"")),
        widget = Some(WidgetsNames.tableChild),
        params = Some(Json.fromFields(Map("sortable" -> Json.True)))
      ),
      CommonField.formFieldChild,
      CommonField.formFieldStatic,
      CommonField.formi18n,
      JSONField(JSONFieldTypes.STATIC,"table_action_title",true,
        label = Some("Table actions"),
        widget = Some(WidgetsNames.h4)
      ),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,4,None,Seq(
          SubLayoutBlock(None,Seq(12,12,12),Seq(
            Right(
              SubLayoutBlock(Some("Base Info"),Seq(12),Seq("name","entity","query","description","guest_user","edit_key_field","show_navigation","props","params").map(Left(_)))
            ),
            Left("")
          ))
        ).map(Right(_))),
        LayoutBlock(Some("Actions"),4,None,Seq("form_actions","table_action_title","form_navigation_actions").map(Left(_))),
        LayoutBlock(Some("I18n"),4,None,Seq("form_i18n").map(Left(_))),
        LayoutBlock(Some("Table Info"),12,None,Seq("tabularFields","exportFields").map(Left(_))),
        LayoutBlock(Some("Fields"),12,None,Seq("fields").map(Left(_))),
        LayoutBlock(Some("Not DB fields"),12,None,Seq("fields_no_db").map(Left(_))),
        LayoutBlock(Some("Linked forms"),12,None,Seq("fields_child").map(Left(_))),
        LayoutBlock(Some("Static elements"),12,None,Seq("fields_static").map(Left(_))),
        LayoutBlock(Some("Layout"),12,None,Seq("layout").map(Left(_))),
      )
    ),
    entity = "form",
    lang = "en",
    tabularFields = Seq("form_uuid","name","entity","description","guest_user"),
    rawTabularFields = Seq("form_uuid","name","entity","description","guest_user"),
    keys = Seq("form_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(
      JSONQuery.filterWith(JSONQueryFilter.WHERE.not("entity",FormMetadataFactory.STATIC_PAGE))
    ),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )


  def page(users:Seq[BoxUser_row]) = JSONMetadata(
    objId = PAGE,
    kind = EntityKind.BOX_FORM.kind,
    name = "page",
    label = "Pages - Interface builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.inputDisabled)),
      CommonField.formName,
      CommonField.formDescription,
      CommonField.formLayout,
      CommonField.formProps,
      CommonField.params,
      JSONField(JSONFieldTypes.STRING,"entity",false,
        widget = Some(WidgetsNames.inputDisabled),
        default = Some(FormMetadataFactory.STATIC_PAGE)
      ),
      JSONField(JSONFieldTypes.STRING,"guest_user",true,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          users.map(x => JSONLookup(x.username.asJson,Seq(x.username)))
        ))
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"show_navigation",false,
        widget = Some(WidgetsNames.hidden),
        default = Some("false")
      ),
      CommonField.formFieldChild,
      CommonField.formFieldStatic,
      CommonField.formi18n,
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,8,None,Seq(
          SubLayoutBlock(None,Seq(12,12,12),Seq(
            Right(
              SubLayoutBlock(Some("Base Info"),Seq(12),Seq("name","description","show_navigation","props","guest_user","params").map(Left(_)))
            ),
          ))
        ).map(Right(_))),
        LayoutBlock(Some("I18n"),4,None,Seq("form_i18n").map(Left(_))),
        LayoutBlock(Some("Linked forms"),12,None,Seq("fields_child").map(Left(_))),
        LayoutBlock(Some("Static elements"),12,None,Seq("fields_static").map(Left(_))),
        LayoutBlock(Some("Layout"),12,None,Seq("layout").map(Left(_))),
      )
    ),
    entity = "form",
    lang = "en",
    tabularFields = Seq("form_uuid","name","description"),
    rawTabularFields = Seq("form_uuid","name","description"),
    keys = Seq("form_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(
      JSONQuery.filterWith(JSONQueryFilter.WHERE.eq("entity",FormMetadataFactory.STATIC_PAGE))
    ),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )


  def field(tables:Seq[String]) = JSONMetadata(
    objId = FORM_FIELD,
    kind = EntityKind.BOX_FORM.kind,
    name = "Field builder",
    label = "Field builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.select), lookup = Some(JSONFieldLookup.withExtractor("entity",CommonField.allFields))),
      CommonField.widget,
      CommonField.typ(false,false),
      JSONField(JSONFieldTypes.BOOLEAN,"required",true,widget = Some(WidgetsNames.checkbox)),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      CommonField.lookupQuery(tables),
      CommonField.default,
      JSONField(JSONFieldTypes.NUMBER,"min",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER,JSONFieldTypes.INTEGER).asJson))
      ),
      JSONField(JSONFieldTypes.NUMBER,"max",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER,JSONFieldTypes.INTEGER).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      CommonField.params,
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,Seq(
          "name",
          "type",
          "widget",
          "required",
          "read_only",
          "lookupEntity",
          "lookupValueField",
          "lookupQuery",
          "default",
          "min",
          "max",
          "conditionFieldId",
          "conditionValues",
          "params"
        ).map(Left(_))),
        LayoutBlock(None,6,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "v_field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","type","widget"),
    rawTabularFields = Seq("name","widget","read_only","lookupEntity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_no_db(tables:Seq[String]) = JSONMetadata(
    objId = FORM_FIELD_NOT_DB,
    kind = EntityKind.BOX_FORM.kind,
    name = "Field builder noDB",
    label = "Field builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.input)),
      CommonField.widget,
      CommonField.typ(false,false),
      JSONField(JSONFieldTypes.BOOLEAN,"required",true,widget = Some(WidgetsNames.checkbox)),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      CommonField.lookupQuery(tables),
      CommonField.default,
      JSONField(JSONFieldTypes.NUMBER,"min",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER).asJson))
      ),
      JSONField(JSONFieldTypes.NUMBER,"max",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      CommonField.params,
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,Seq(
          "name",
          "type",
          "widget",
          "required",
          "read_only",
          "lookupEntity",
          "lookupValueField",
          "lookupQuery",
          "default",
          "min",
          "max",
          "conditionFieldId",
          "conditionValues",
          "params"
        ).map(Left(_))),
        LayoutBlock(None,6,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "v_field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","type","widget"),
    rawTabularFields = Seq("name","widget","read_only","lookupEntity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_childs(forms:Seq[BoxForm.BoxForm_row]) = JSONMetadata(
    objId = FORM_FIELD_CHILDS,
    kind = EntityKind.BOX_FORM.kind,
    name = "Field builder childs",
    label = "Field builder childs",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.name,
      JSONField(JSONFieldTypes.STRING,"widget",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          WidgetsNames.mapping(JSONFieldTypes.CHILD).map(x => JSONLookup(x.asJson,Seq(x)))
        )
      )),
      JSONField(JSONFieldTypes.STRING,"type",false,
        widget = Some(WidgetsNames.hidden),
        default = Some(JSONFieldTypes.CHILD)
      ),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      JSONField(JSONFieldTypes.STRING,"child_form_uuid",false,
        label = Some("Child form"),
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          forms.map{ form => JSONLookup(form.form_uuid.get.asJson,Seq(form.name)) }.sortBy(_.value)
        ))
      ),
      JSONField(JSONFieldTypes.STRING,"masterFields",false,
        label = Some("Parent key fields"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.lookupForm,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
        ).asJson))
      ),
      JSONField(JSONFieldTypes.STRING,"childFields",false,
        label = Some("Child key fields"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
        ).asJson))
      ),
      JSONField(JSONFieldTypes.STRING,"childQuery",true,
        widget = Some(WidgetsNames.code),
        params = Some(Json.obj("language" -> "json".asJson, "height" -> 200.asJson)),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.linkedForm,
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
        ).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code)),
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,Seq(
          "name",
          "type",
          "widget",
          "read_only",
          "child_form_uuid",
          "masterFields",
          "childFields",
          "childQuery",
          "default",
          "conditionFieldId",
          "conditionValues",
          "params"
        ).map(Left(_))),
        LayoutBlock(None,6,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","widget"),
    rawTabularFields = Seq("name","widget","read_only","lookupEntity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_static(tables:Seq[String],functions:Seq[String]) = JSONMetadata(
    objId = FORM_FIELD_STATIC,
    kind = EntityKind.BOX_FORM.kind,
    name = "Field builder static",
    label = "Field builder static",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.name,
      JSONField(JSONFieldTypes.STRING,"widget",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          WidgetsNames.mapping(JSONFieldTypes.STATIC).map(x => JSONLookup(x.asJson,Seq(x)))
        )
      )),
      JSONField(JSONFieldTypes.STRING,"type",false,
        widget = Some(WidgetsNames.hidden),
        default = Some(JSONFieldTypes.STATIC)
      ),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      JSONField(JSONFieldTypes.STRING,"masterFields",false,label=Some("Parent field"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.lookupLabel).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code)),
      JSONField(JSONFieldTypes.STRING,"function",false,label=Some("Function"),
        widget = Some(WidgetsNames.select),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.executeFunction).asJson)),
        lookup = Some(JSONFieldLookup.prefilled(
          functions.sorted.map(x => JSONLookup(x.asJson,Seq(x)))
        ))
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.hidden))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,Seq(
          "name",
          "type",
          "widget",
          "lookupEntity",
          "masterFields",
          "function",
          "lookupValueField",
          "lookupQuery",
          "conditionFieldId",
          "conditionValues",
          "params",
          "read_only"
        ).map(Left(_))),
        LayoutBlock(None,6,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","widget"),
    rawTabularFields = Seq("name","widget","read_only","lookupEntity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def fieldI18n(langs:Seq[String]) = JSONMetadata(
    objId = FORM_FIELD_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "FieldI18n builder",
    label = "FieldI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false, widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false, widget = Some(WidgetsNames.hidden)),
      CommonField.lang(langs),
      CommonField.label(),
      CommonField.tooltip,
      CommonField.hint,
      CommonField.placeholder(Seq(
        WidgetsNames.input,
        WidgetsNames.textarea,
        WidgetsNames.code,
        WidgetsNames.richTextEditorFull,
        WidgetsNames.richTextEditor,
        WidgetsNames.redactor
      )),
      CommonField.lookupTextField(Seq(WidgetsNames.select,WidgetsNames.popup,WidgetsNames.multipleLookup,WidgetsNames.linkedForm,WidgetsNames.lookupForm,WidgetsNames.lookupLabel,WidgetsNames.input)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq("lang","label","lookupTextField","placeholder","tooltip").map(Left(_))),
      )
    ),
    entity = "field_i18n",
    lang = "en",
    tabularFields = Seq("field_uuid","uuid","lang","label"),
    rawTabularFields = Seq("lang","label","lookupTextField"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("lang"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def formI18n(views:Seq[String],langs:Seq[String]) = JSONMetadata(
    objId = FORM_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "FormI18n builder",
    label = "FormI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.lang(langs),
      CommonField.simpleLabel,
      JSONField(JSONFieldTypes.STRING,"view_table",true,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          views.map(x => JSONLookup(x.asJson,Seq(x)))
        ))
      ),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq("lang","label","view_table").map(Left(_))),
      )
    ),
    entity = "form_i18n",
    lang = "en",
    tabularFields = Seq("form_uuid","uuid","lang","label"),
    rawTabularFields = Seq("lang","label","view_table"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("lang"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def form_actions(functions:Seq[String]) = JSONMetadata(
    objId = FORM_ACTION,
    kind = EntityKind.BOX_FORM.kind,
    name = "Form action",
    label = "Form action",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.NUMBER,"action_order",false,widget = Some(WidgetsNames.hidden),default = Some("arrayIndex")),
      JSONField(JSONFieldTypes.STRING,"action",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          Action.all.map(x => JSONLookup(x.toString.asJson,Seq(x.toString)))
        )
        )),
      JSONField(JSONFieldTypes.STRING,"importance",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          Importance.all.map(x => JSONLookup(x.toString.asJson,Seq(x.toString)))
        )
        )),
      JSONField(JSONFieldTypes.STRING,"after_action_goto",true,
        widget = Some(WidgetsNames.input),
        tooltip = Some(
          """
            |the goto action is the path were we want to go after the action
            |the following subsititution are applied
            |$kind -> the kind of the form i.e. 'table' or 'form'
            |$name -> name of the current form/table
            |$id -> id of the current/saved record
            |$writable -> if the current form is writable
            |""".stripMargin)
      ),
      JSONField(JSONFieldTypes.STRING,"label",false,label=Some("Label"),
        tooltip = Some("Use global translation table"),
        widget = Some(WidgetsNames.input)
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"update_only",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.BOOLEAN,"insert_only",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.BOOLEAN,"reload",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.BOOLEAN,"html_check",false,widget = Some(WidgetsNames.checkbox), default = Some("true")),
      JSONField(JSONFieldTypes.STRING,"confirm_text",true,label=Some("Confirm text"),
        tooltip = Some("Before running action show a popup that ask for confirmation with the following text (translated with gobal transaltion table)"),
        widget = Some(WidgetsNames.input)
      ),
      JSONField(JSONFieldTypes.STRING,"execute_function",true,label=Some("Function"),
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          functions.sorted.map(x => JSONLookup(x.asJson,Seq(x)))
        ))
      ),
      JSONField(JSONFieldTypes.JSON,"condition",true,
        widget = Some(WidgetsNames.code),
        placeholder = Some("[1,2,3]"),
        tooltip = Some("Enter a JSON array that express the condition"),
        params = Some(Json.obj("language" -> "json".asJson, "height" -> 100.asJson, "fullWidth" -> true.asJson))
      )
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq(
          "uuid",
          "form_uuid",
          "order",
          "action",
          "importance",
          "execute_function",
          "after_action_goto",
          "label",
          "update_only",
          "insert_only",
          "reload",
          "confirm_text",
          "condition",
          "html_check"
        ).map(Left(_))),
      )
    ),
    entity = "form_actions",
    lang = "en",
    tabularFields = Seq("label","action","importance","execute_function","after_action_goto"),
    rawTabularFields = Seq("label","action","importance","execute_function","after_action_goto"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("action_order"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )


  def form_navigation_actions(functions:Seq[String]) = JSONMetadata(
    objId = FORM_NAVIGATION_ACTION,
    kind = EntityKind.BOX_FORM.kind,
    name = "Form navigation action",
    label = "Form navigation action",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.NUMBER,"action_order",false,widget = Some(WidgetsNames.hidden), default = Some("arrayIndex")),
      JSONField(JSONFieldTypes.STRING,"action",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          Action.all.map(x => JSONLookup(x.toString.asJson,Seq(x.toString)))
        )
        )),
      JSONField(JSONFieldTypes.STRING,"importance",false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          Importance.all.map(x => JSONLookup(x.toString.asJson,Seq(x.toString)))
        )
        )),
      JSONField(JSONFieldTypes.STRING,"after_action_goto",true,
        widget = Some(WidgetsNames.input),
        tooltip = Some(
          """
            |the goto action is the path were we want to go after the action
            |the following subsititution are applied
            |$kind -> the kind of the form i.e. 'table' or 'form'
            |$name -> name of the current form/table
            |$id -> id of the current/saved record
            |$writable -> if the current form is writable
            |""".stripMargin)
      ),
      JSONField(JSONFieldTypes.STRING,"label",false,label=Some("Label"),
        tooltip = Some("Use global translation table"),
        widget = Some(WidgetsNames.input)
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"update_only",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.BOOLEAN,"insert_only",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.BOOLEAN,"reload",false,widget = Some(WidgetsNames.checkbox), default = Some("false")),
      JSONField(JSONFieldTypes.STRING,"confirm_text",true,label=Some("Confirm text"),
        tooltip = Some("Before running action show a popup that ask for confirmation with the following text (translated with gobal transaltion table)"),
        widget = Some(WidgetsNames.input)
      ),
      JSONField(JSONFieldTypes.STRING,"execute_function",true,label=Some("Function"),
        widget = Some(WidgetsNames.select),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.executeFunction).asJson)),
        lookup = Some(JSONFieldLookup.prefilled(
          functions.sorted.map(x => JSONLookup(x.asJson,Seq(x)))
        ))
      ),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq(
          "uuid",
          "form_uuid",
          "order",
          "action",
          "importance",
          "execute_function",
          "after_action_goto",
          "label",
          "update_only",
          "insert_only",
          "reload",
          "confirm_text"
        ).map(Left(_))),
      )
    ),
    entity = "form_navigation_actions",
    lang = "en",
    tabularFields = Seq("label","action","importance","execute_function","after_action_goto"),
    rawTabularFields = Seq("label","action","importance","execute_function","after_action_goto"),
    keys = Seq("uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("action_order"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )
}
