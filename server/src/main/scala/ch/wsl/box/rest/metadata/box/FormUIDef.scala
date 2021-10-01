package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.boxentities.BoxForm
import ch.wsl.box.model.boxentities.BoxUser.BoxUser_row
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.utils.BoxConfig

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
          tables.map(x => JSONLookup(x.asJson,x))
        ))
      ),
      JSONField(JSONFieldTypes.BOOLEAN,"show_navigation",false,
        widget = Some(WidgetsNames.checkbox),
        default = Some("true")
      ),
      JSONField(JSONFieldTypes.STRING,"tabularFields",false,widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.STRING,"query",true,
        widget = Some(WidgetsNames.code),
        params = Some(Json.obj("language" -> "json".asJson, "height" -> 100.asJson, "fullWidth" -> false.asJson))
      ),
      JSONField(JSONFieldTypes.STRING,"guest_user",true,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          users.map(x => JSONLookup(x.username.asJson,x.username))
        ))
      ),
      JSONField(JSONFieldTypes.STRING,"edit_key_field",true,
        widget = Some(WidgetsNames.input),
        label = Some("Key fields"),
        placeholder = Some("by default primary key is used"),
        tooltip = Some("Manually enter the fields that should be used as primary key. This is useful mainly for updatable views where the primary key of the entity cannot be calculated. Fields are separated with comma")
      ),
      JSONField(JSONFieldTypes.STRING,"exportFields",true,widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.CHILD,"fields",true,
        child = Some(Child(FORM_FIELD,"fields","form_uuid","form_uuid",
          Some(JSONQuery.sortByKeys(Seq("name")).filterWith(JSONQueryFilter("type",Some("notin"),JSONFieldTypes.STATIC+","+JSONFieldTypes.CHILD))),
          ""
        )),
        widget = Some(WidgetsNames.tableChild)
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
              SubLayoutBlock(Some("Base Info"),Seq(12),Seq("name","entity","query","description","guest_user","edit_key_field","show_navigation","props","params").map(Left(_)))
            ),
            Left(""),
            Right(
              SubLayoutBlock(Some("Table Info"),Seq(12),Seq("tabularFields","exportFields").map(Left(_)))
            )
          ))
        ).map(Right(_))),
        LayoutBlock(Some("I18n"),4,None,Seq("form_i18n").map(Left(_))),
        LayoutBlock(Some("Fields"),12,None,Seq("fields").map(Left(_))),
        LayoutBlock(Some("Linked forms"),12,None,Seq("fields_child").map(Left(_))),
        LayoutBlock(Some("Static elements"),12,None,Seq("fields_static").map(Left(_))),
        LayoutBlock(Some("Layout"),12,None,Seq("layout").map(Left(_))),
      )
    ),
    entity = "form",
    lang = "en",
    tabularFields = Seq("form_uuid","name","entity","description"),
    rawTabularFields = Seq("form_uuid","name","entity","description"),
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
          users.map(x => JSONLookup(x.username.asJson,x.username))
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
      CommonField.name,
      CommonField.widget,
      CommonField.typ(false,false),
      JSONField(JSONFieldTypes.BOOLEAN,"required",true,widget = Some(WidgetsNames.checkbox)),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      JSONField(JSONFieldTypes.CHILD,"field_file",true,
        child = Some(Child(FORM_FIELD_FILE,"field_file","field_uuid","field_uuid",None,"")),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.FILE.asJson))),
        params = Some(Map("max" -> 1, "min" -> 0).asJson),
        widget = Some(WidgetsNames.simpleChild)
      ),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      CommonField.lookupQuery(tables),
      CommonField.default,
      JSONField(JSONFieldTypes.NUMBER,"min",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER.asJson)))
      ),
      JSONField(JSONFieldTypes.NUMBER,"max",true,
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("type",Seq(JSONFieldTypes.NUMBER.asJson)))
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
          "params",
          "field_file"
        ).map(Left(_))),
        LayoutBlock(None,6,None,Seq("field_i18n").map(Left(_))),
      )
    ),
    entity = "field",
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
          WidgetsNames.mapping(JSONFieldTypes.CHILD).map(x => JSONLookup(x.asJson,x))
        )
      )),
      JSONField(JSONFieldTypes.STRING,"type",false,
        widget = Some(WidgetsNames.hidden),
        default = Some(JSONFieldTypes.CHILD)
      ),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      JSONField(JSONFieldTypes.STRING,"child_form_uuid",true,
        label = Some("Child form"),
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          forms.map{ form => JSONLookup(form.form_uuid.get.asJson,form.name) }.sortBy(_.value)
        ))
      ),
      JSONField(JSONFieldTypes.STRING,"masterFields",true,
        label = Some("Parent key fields"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild.asJson,
          WidgetsNames.tableChild.asJson,
          WidgetsNames.lookupForm.asJson,
          WidgetsNames.editableTable.asJson,
          WidgetsNames.trasparentChild.asJson,
        )))
      ),
      JSONField(JSONFieldTypes.STRING,"childFields",true,
        label = Some("Child key fields"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.simpleChild.asJson,
          WidgetsNames.tableChild.asJson,
          WidgetsNames.editableTable.asJson,
          WidgetsNames.trasparentChild.asJson,
        )))
      ),
      JSONField(JSONFieldTypes.STRING,"childQuery",true,
        widget = Some(WidgetsNames.code),
        params = Some(Json.obj("language" -> "json".asJson, "height" -> 200.asJson)),
        condition = Some(ConditionalField("widget",Seq(
          WidgetsNames.linkedForm.asJson,
          WidgetsNames.simpleChild.asJson,
          WidgetsNames.tableChild.asJson,
          WidgetsNames.editableTable.asJson,
          WidgetsNames.trasparentChild.asJson,
        )))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code)),
      JSONField(JSONFieldTypes.BOOLEAN,"read_only",false,default = Some("false"),widget = Some(WidgetsNames.checkbox)),
      JSONField(JSONFieldTypes.CHILD,"field_file",true,
        child = Some(Child(FORM_FIELD_FILE,"field_file","field_uuid","field_uuid",None,""))
      )
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
          "params",
          "field_file"
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
          WidgetsNames.mapping(JSONFieldTypes.STATIC).map(x => JSONLookup(x.asJson,x))
        )
      )),
      JSONField(JSONFieldTypes.STRING,"type",false,
        widget = Some(WidgetsNames.hidden),
        default = Some(JSONFieldTypes.STATIC)
      ),
      JSONField(JSONFieldTypes.CHILD,"field_i18n",true,child = Some(Child(FORM_FIELD_I18N,"field_i18n","field_uuid","field_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"widget")), widget = Some(WidgetsNames.tableChild)),
      CommonField.lookupEntity(tables),
      CommonField.lookupValueField(tables),
      JSONField(JSONFieldTypes.STRING,"masterFields",true,label=Some("Parent field"),
        widget = Some(WidgetsNames.input),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.lookupLabel.asJson)))
      ),
      CommonField.conditionFieldId,
      CommonField.conditionValues,
      JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code)),
      JSONField(JSONFieldTypes.CHILD,"field_file",true,
        child = Some(Child(FORM_FIELD_FILE,"field_file","field_uuid","field_uuid",None,""))
      ),
      JSONField(JSONFieldTypes.STRING,"function",true,label=Some("Function"),
        widget = Some(WidgetsNames.select),
        condition = Some(ConditionalField("widget",Seq(WidgetsNames.executeFunction.asJson))),
        lookup = Some(JSONFieldLookup.prefilled(
          functions.sorted.map(x => JSONLookup(x.asJson,x))
        ))
      ),
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

  val fieldI18n = JSONMetadata(
    objId = FORM_FIELD_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "FieldI18n builder",
    label = "FieldI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false, widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false, widget = Some(WidgetsNames.hidden)),
      CommonField.lang,
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
      CommonField.lookupTextField(Seq(WidgetsNames.select,WidgetsNames.popup,WidgetsNames.linkedForm,WidgetsNames.lookupForm,WidgetsNames.lookupLabel,WidgetsNames.input)),
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

  def formI18n(views:Seq[String]) = JSONMetadata(
    objId = FORM_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "FormI18n builder",
    label = "FormI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"form_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"uuid",false,widget = Some(WidgetsNames.hidden)),
      CommonField.lang,
      CommonField.simpleLabel,
      JSONField(JSONFieldTypes.STRING,"view_table",true,
        widget = Some(WidgetsNames.select),
        lookup = Some(JSONFieldLookup.prefilled(
          views.map(x => JSONLookup(x.asJson,x))
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

  val fieldFile = JSONMetadata(
    objId = FORM_FIELD_FILE,
    kind = EntityKind.BOX_FORM.kind,
    name = "FieldFile builder",
    label = "FieldFile builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"field_uuid",false,widget = Some(WidgetsNames.hidden)),
      JSONField(JSONFieldTypes.STRING,"file_field",false,widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.STRING,"thumbnail_field",false,widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.STRING,"name_field",false,widget = Some(WidgetsNames.input)),
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq("file_field","thumbnail_field","name_field").map(Left(_))),
      )
    ),
    entity = "field_file",
    lang = "en",
    tabularFields = Seq("field_uuid","file_field"),
    rawTabularFields = Seq("field_uuid","file_field"),
    keys = Seq("field_uuid"),
    keyStrategy = NaturalKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )
}
