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
      CommonField.formParamsLayout,
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
      JSONField(JSONFieldTypes.BOOLEAN,"public_list",false,
        widget = Some(WidgetsNames.checkbox),
        default = Some("false")
      ),
      JSONField(JSONFieldTypes.STRING,"tabularFields",false,widget = Some(WidgetsNames.textarea)),
      JSONField(JSONFieldTypes.JSON,"query",true,
        widget = Some(WidgetsNames.popupWidget),
        params = Some(Json.obj(
          "widget" -> WidgetsNames.adminQueryBuilder.asJson,
          "entity" -> s"${Widget.REF}entity".asJson,
          "avoidShorten" -> Json.True
        ))
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
      JSONField(JSONFieldTypes.STRING,"exportfields",true,widget = Some(WidgetsNames.textarea)),
      JSONField(JSONFieldTypes.CHILD,"fields",true,
        child = Some(Child(FORM_FIELD,"fields",Seq("form_uuid"),Seq("form_uuid"),
          Some(JSONQuery.sortByKeys(Seq("name")).filterWith(
            JSONQueryFilter.withValue("type",Some("notin"),JSONFieldTypes.STATIC+","+JSONFieldTypes.CHILD),
            JSONQueryFilter.WHERE.eq("entity_field","true")
          )),
          props = "entity",
          true
        )),
        widget = Some(WidgetsNames.tableChild)
      ),
      JSONField(JSONFieldTypes.CHILD,"fields_no_db",true,
        child = Some(Child(FORM_FIELD_NOT_DB,"fields_no_db",Seq("form_uuid"),Seq("form_uuid"),
          Some(JSONQuery.sortByKeys(Seq("name")).filterWith(
            JSONQueryFilter.withValue("type",Some("notin"),JSONFieldTypes.STATIC+","+JSONFieldTypes.CHILD),
            JSONQueryFilter.WHERE.eq("entity_field","false")
          )),
          props = "entity",
          true
        )),
        widget = Some(WidgetsNames.tableChild)
      ),
      JSONField(JSONFieldTypes.CHILD,"form_actions",true,
        child = Some(Child(FORM_ACTION,"form_actions",Seq("form_uuid"),Seq("form_uuid"), None,"",true)),
        widget = Some(WidgetsNames.tableChild),
        params = Some(Json.fromFields(Map("sortable" -> Json.True)))
      ),
      JSONField(JSONFieldTypes.CHILD,"form_navigation_actions",true,
        child = Some(Child(FORM_NAVIGATION_ACTION,"form_navigation_actions",Seq("form_uuid"),Seq("form_uuid"), None,"",true)),
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
        LayoutBlock(Some(Left("Base Info")),4,None,None,None,Seq(
          "name",
          "entity",
          "edit_key_field",
          "query",
          "layout",
          "description",
          "guest_user",
          "public_list",
          "props",
          "params"
        ).map(Left(_))),
        LayoutBlock(Some(Left("I18n")),4,None,None,None,Seq("form_i18n").map(Left(_))),
        LayoutBlock(Some(Left("Actions")),4,None,None,None,Seq("show_navigation","form_actions","table_action_title","form_navigation_actions").map(Left(_))),
        LayoutBlock(Some(Left("Table Info")),12,None,None,None,Seq("tabularFields","exportfields").map(Left(_))),
        LayoutBlock(Some(Left("Fields")),12,None,None,None,Seq("fields").map(Left(_))),
        LayoutBlock(Some(Left("Not DB fields")),12,None,None,None,Seq("fields_no_db").map(Left(_))),
        LayoutBlock(Some(Left("Linked forms")),12,None,None,None,Seq("fields_child").map(Left(_))),
        LayoutBlock(Some(Left("Static elements")),12,None,None,None,Seq("fields_static").map(Left(_)))
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
        LayoutBlock(None,8,None,None,None,Seq(
          SubLayoutBlock(None,Some(Seq(12,12,12)),Seq(
            Right(
              SubLayoutBlock(Some(Left("Base Info")),Some(Seq(12)),Seq("name","description","show_navigation","props","guest_user","params").map(Left(_)))
            ),
          ))
        ).map(Right(_))),
        LayoutBlock(Some(Left("I18n")),4,None,None,None,Seq("form_i18n").map(Left(_))),
        LayoutBlock(Some(Left("Linked forms")),12,None,None,None,Seq("fields_child").map(Left(_))),
        LayoutBlock(Some(Left("Static elements")),12,None,None,None,Seq("fields_static").map(Left(_))),
        LayoutBlock(Some(Left("Layout")),12,None,None,None,Seq("layout").map(Left(_))),
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


  def field(tables:Seq[String],fields:Map[String, Seq[String]]) = JSONMetadata(
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
      CommonField.fieldi18n,
      CommonField.foreignEntity(tables),
      CommonField.foreignValueField(tables,fields),
      CommonField.foreignQuery(tables),
      CommonField.roles,
      CommonField.default,
      JSONField(JSONFieldTypes.NUMBER,"min",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER,JSONFieldTypes.INTEGER,JSONFieldTypes.ARRAY_NUMBER).asJson))
      ),
      JSONField(JSONFieldTypes.NUMBER,"max",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER,JSONFieldTypes.INTEGER,JSONFieldTypes.ARRAY_NUMBER).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      CommonField.params,
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,None,None,Seq(
          "name",
          "type",
          "widget",
          "required",
          "read_only",
          "foreign_entity",
          "foreign_value_field",
          "lookupQuery",
          "default",
          "min",
          "max",
          "conditionFieldId",
          "conditionValues",
          "params",
          "roles"
        ).map(Left(_))),
        LayoutBlock(None,6,None,None,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "v_field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","type","widget"),
    rawTabularFields = Seq("name","widget","read_only","foreign_entity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_no_db(tables:Seq[String],fields:Map[String, Seq[String]]) = JSONMetadata(
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
      CommonField.fieldi18n,
      CommonField.foreignEntity(tables),
      CommonField.foreignValueField(tables,fields),
      CommonField.foreignQuery(tables),
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
        LayoutBlock(None,6,None,None,None,Seq(
          "name",
          "type",
          "widget",
          "required",
          "read_only",
          "foreign_entity",
          "foreign_value_field",
          "lookupQuery",
          "default",
          "min",
          "max",
          "conditionFieldId",
          "conditionValues",
          "params"
        ).map(Left(_))),
        LayoutBlock(None,6,None,None,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "v_field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","type","widget"),
    rawTabularFields = Seq("name","widget","read_only","foreign_entity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_childs(forms:Seq[BoxForm.BoxForm_row],fields:Map[String, Seq[String]]) = JSONMetadata(
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
      CommonField.fieldi18n,
      JSONField(JSONFieldTypes.STRING,"child_form_uuid",false,
        label = Some("Child form"),
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          forms.map{ form => JSONLookup(form.form_uuid.get.asJson,Seq(form.name)) }.sortBy(_.value)
        ))
      ),
      JSONField(JSONFieldTypes.CHILD,"formRef",true,label = Some("Open form"),
        widget = Some(WidgetsNames.lookupForm),
        child = Some(Child(Constants.FORM,"form_uuid",Seq("child_form_uuid"),Seq("form_uuid"),None,"",false)),
        linked = Some(LinkedForm("form",Seq("child_form_uuid"),Seq("form_uuid"),None,Some("Open child form"),EntityKind.BOX_FORM)),
        params = Some(Json.fromFields(Map("target" -> Json.fromString("new_window"))))
      ),
      JSONField(JSONFieldTypes.ARRAY_STRING,"local_key_columns",false,
        label = Some("Parent key fields"),
        widget = Some(WidgetsNames.multipleLookup),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.lookupForm,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
          WidgetsNames.spreadsheet,
        ).asJson)),
        lookup =  Some(JSONFieldLookup.withExtractor(
          "entity",
          fields.map{ case (t,f) => t.asJson ->  f.map(x => JSONLookup(x.asJson,Seq(x)))}
        ))
      ),
      JSONField(JSONFieldTypes.ARRAY_STRING,"foreign_key_columns",false,
        label = Some("Child key fields"),
        widget = Some(WidgetsNames.multipleLookup),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
          WidgetsNames.spreadsheet,
        ).asJson)),
        lookup = Some(JSONFieldLookup.withExtractor(
          "child_form_uuid",
          {
            forms.map( f => f.form_uuid.get.asJson -> fields.get(f.entity).toList.flatten.map(x => JSONLookup(x.asJson,Seq(x))))
          }.toMap
        ))
      ),
      JSONField(JSONFieldTypes.JSON,"childQuery",true,
        widget = Some(WidgetsNames.popupWidget),
        params = Some(Json.obj(
          "widget" -> WidgetsNames.adminQueryBuilder.asJson,
          "form" -> s"${Widget.REF}child_form_uuid".asJson,
          "avoidShorten" -> Json.True
        )),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.linkedForm,
          WidgetsNames.simpleChild,
          WidgetsNames.tableChild,
          WidgetsNames.editableTable,
          WidgetsNames.trasparentChild,
          WidgetsNames.spreadsheet,
        ).asJson))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code)),
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,6,None,None,None,Seq(
          "name",
          "type",
          "widget",
          "read_only",
          "child_form_uuid",
          "formRef",
          "local_key_columns",
          "foreign_key_columns",
          "childQuery",
          "default",
          "conditionFieldId",
          "conditionValues",
          "params"
        ).map(Left(_))),
        LayoutBlock(None,6,None,None,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","widget"),
    rawTabularFields = Seq("name","widget","read_only","foreign_entity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def field_static(tables:Seq[String],functions:Seq[String],fields:Map[String, Seq[String]]) = JSONMetadata(
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
      CommonField.fieldi18n,
      CommonField.foreignEntity(tables),
      CommonField.foreignValueField(tables,fields),
      JSONField(JSONFieldTypes.ARRAY_STRING,"local_key_columns",false,label=Some("Parent field"),
        widget = Some(WidgetsNames.inputMultipleText),
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
        LayoutBlock(None,6,None,None,None,Seq(
          "name",
          "type",
          "widget",
          "foreign_entity",
          "local_key_columns",
          "function",
          "foreign_value_field",
          "lookupQuery",
          "conditionFieldId",
          "conditionValues",
          "params",
          "read_only"
        ).map(Left(_))),
        LayoutBlock(None,6,None,None,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "field",
    lang = "en",
    tabularFields = Seq("field_uuid","name","widget"),
    rawTabularFields = Seq("name","widget","read_only","foreign_entity","child_form_uuid"),
    keys = Seq("field_uuid"),
    keyStrategy = SurrugateKey,
    query = Some(JSONQuery.sortByKeys(Seq("name"))),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def fieldI18n(langs:Seq[String],fields:Map[String, Seq[String]]) = JSONMetadata(
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
      JSONField(JSONFieldTypes.ARRAY_STRING,"foreign_label_columns",true,
        label = Some("Foreign label field"),
        tooltip = Some("Label in external table"),
        widget = Some(WidgetsNames.multipleLookup),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.select,WidgetsNames.popup,WidgetsNames.multipleLookup,WidgetsNames.linkedForm,WidgetsNames.lookupForm,WidgetsNames.lookupLabel,WidgetsNames.input,WidgetsNames.multi,WidgetsNames.popupWidget).asJson)),
        lookup = Some(JSONFieldLookup.withExtractor(
          "foreign_entity",
          fields.map{ case (t,f) => t.asJson ->  f.map(x => JSONLookup(x.asJson,Seq(x)))}
        ))
      ),
      JSONField(JSONFieldTypes.STRING,"dynamic_label",true,
        label = Some("Dynamic label"),
        tooltip = Some("Another field in the same form"),
        widget = Some(WidgetsNames.hidden),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.linkedForm,WidgetsNames.lookupForm,WidgetsNames.lookupLabel,WidgetsNames.multi,WidgetsNames.popupWidget).asJson))
      )
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,None,None,Seq("lang","label","foreign_label_columns","dynamic_label","placeholder","tooltip").map(Left(_))),
      )
    ),
    entity = "field_i18n",
    lang = "en",
    tabularFields = Seq("field_uuid","uuid","lang","label"),
    rawTabularFields = Seq("lang","label","foreign_value_field"),
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
        LayoutBlock(None,12,None,None,None,Seq("lang","label","view_table").map(Left(_))),
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
      JSONField(JSONFieldTypes.STRING, "target", false,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          Target.all.map(x => JSONLookup(x.toString.asJson, Seq(x.toString)))
        )
        )).copy(default = Some("Self")),
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
        LayoutBlock(None,12,None,None,None,Seq(
          "uuid",
          "form_uuid",
          "order",
          "action",
          "importance",
          "execute_function",
          "after_action_goto",
          "target",
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
        LayoutBlock(None,12,None,None,None,Seq(
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
