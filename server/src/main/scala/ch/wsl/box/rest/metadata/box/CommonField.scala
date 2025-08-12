package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared.{Child, ConditionalField, JSONField, JSONFieldLookup, JSONFieldTypes, JSONLookup, JSONQuery, JSONQueryFilter, Widget, WidgetsNames}
import ch.wsl.box.rest.metadata.box.Constants.{FORM_FIELD_CHILDS, FORM_FIELD_FILE, FORM_FIELD_I18N, FORM_FIELD_STATIC, FORM_I18N}
import ch.wsl.box.rest.runtime.Registry
import io.circe.Json
import io.circe.syntax._

object CommonField {

  val allFields:Map[Json,Seq[JSONLookup]] = Registry().fields.tableFields.map{ case (table,fields) =>
    table.asJson -> fields.keys.toSeq.sorted.map(field => JSONLookup(field.asJson,Seq(field)))
  }

  val name = JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.input))

  val widget = JSONField(JSONFieldTypes.STRING,"widget",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.withExtractor(
      "type",
      WidgetsNames.mapping.map{ case (k,v) => k.asJson -> v.map(x => JSONLookup(x.asJson,Seq(x)))}
    )
  ))

  def typ(child:Boolean = true, static:Boolean = true) = JSONField(JSONFieldTypes.STRING,"type",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      JSONFieldTypes.ALL
        .filter(x => child || x != JSONFieldTypes.CHILD)
        .filter(x => static || x != JSONFieldTypes.STATIC)
        .sorted.map(x => JSONLookup(x.asJson,Seq(x)))
    )
  ))

  def lookupEntity(tables:Seq[String]) =  JSONField(JSONFieldTypes.STRING,"lookupEntity",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      tables.map(x => JSONLookup(x.asJson,Seq(x)))
    )),
    condition = Some(ConditionalField("widget",Seq(WidgetsNames.select,WidgetsNames.popup,WidgetsNames.lookupLabel,WidgetsNames.multipleLookup, WidgetsNames.multi, WidgetsNames.popupWidget).asJson))
  )

  def foreignEntity(tables:Seq[String]) =  JSONField(JSONFieldTypes.STRING,"foreign_entity",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      tables.map(x => JSONLookup(x.asJson,Seq(x)))
    )),
    condition = Some(ConditionalField("widget",Seq(WidgetsNames.select,WidgetsNames.popup,WidgetsNames.lookupLabel,WidgetsNames.multipleLookup, WidgetsNames.multi, WidgetsNames.popupWidget).asJson))
  )

  def lookupValueField(tables:Seq[String]) =  JSONField(JSONFieldTypes.STRING,"lookupValueField",false,
    condition = Some(ConditionalField("lookupEntity",tables.asJson)),
    widget = Some(WidgetsNames.input)
  )

  def foreignValueField(tables:Seq[String],fields:Map[String, Seq[String]]) =  JSONField(JSONFieldTypes.STRING,"foreign_value_field",false,
    condition = Some(ConditionalField("foreign_entity",tables.asJson)),
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.withExtractor(
      "foreign_entity",
      fields.map{ case (t,f) => t.asJson ->  f.map(x => JSONLookup(x.asJson,Seq(x)))}
    ))
  )

  def lookupQuery(tables:Seq[String]) = JSONField(JSONFieldTypes.JSON,"lookupQuery",true,
    condition = Some(ConditionalField("lookupEntity",tables.asJson)),
    widget = Some(WidgetsNames.popupWidget),
    params = Some(Json.obj(
      "widget" -> WidgetsNames.adminQueryBuilder.asJson,
      "entity" -> s"${Widget.REF}lookupEntity".asJson,
      "avoidShorten" -> Json.True
    ))
  )

  def foreignQuery(tables:Seq[String]) = JSONField(JSONFieldTypes.JSON,"lookupQuery",true,
    condition = Some(ConditionalField("foreign_entity",tables.asJson)),
    widget = Some(WidgetsNames.popupWidget),
    params = Some(Json.obj(
      "widget" -> WidgetsNames.adminQueryBuilder.asJson,
      "entity" -> s"${Widget.REF}foreign_entity".asJson,
      "avoidShorten" -> Json.True
    ))
  )

  val default = JSONField(JSONFieldTypes.STRING,"default",true,widget = Some(WidgetsNames.input), tooltip = Some("Use keyword `arrayIndex` to substitute the value with the index of the array (when this field is part of a child)"))

  val conditionFieldId = JSONField(JSONFieldTypes.STRING,"conditionFieldId",true,widget = Some(WidgetsNames.input))
  val conditionValues = JSONField(JSONFieldTypes.STRING,"conditionValues",true,
    widget = Some(WidgetsNames.code),
    placeholder = Some("[1,2,3]"),
    tooltip = Some("Enter a JSON array with the possibles values"),
    params = Some(Json.obj("language" -> "json".asJson, "height" -> 50.asJson, "fullWidth" -> false.asJson))
  )

  val roles = JSONField(JSONFieldTypes.ARRAY_STRING,"roles",true,
    widget = Some(WidgetsNames.inputMultipleText)
  )

  def lang(langs:Seq[String]) = JSONField(JSONFieldTypes.STRING,"lang",false,

    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      langs.map(x => JSONLookup(x.asJson,Seq(x)))
    ))
  )


  val simpleLabel = JSONField(JSONFieldTypes.STRING,"label",true, widget = Some(WidgetsNames.input))

  def label(widgetDisabled:Seq[String] = Seq()) = JSONField(JSONFieldTypes.STRING,"label",true, widget = Some(WidgetsNames.dynamicWidget),
    condition = Some(ConditionalField("widget",WidgetsNames.all.diff(widgetDisabled).asJson)),
    params = Some(Json.obj(
      "selectorField" -> "widget".asJson,
      "widgetMapping" -> Json.obj(
        "html" -> Json.obj("name" -> WidgetsNames.code.asJson, "params" -> Json.obj("language" -> "html".asJson))
      ),
      "default" -> Json.obj("name" -> WidgetsNames.input.asJson)
    ))
  )
  val tooltip = JSONField(JSONFieldTypes.STRING,"tooltip",true, widget = Some(WidgetsNames.input))
  val hint = JSONField(JSONFieldTypes.STRING,"hint",true, widget = Some(WidgetsNames.input))
  def placeholder(widgetEnabled:Seq[String] = Seq()) = JSONField(JSONFieldTypes.STRING,"placeholder",true, widget = Some(WidgetsNames.input),
    condition = Some(ConditionalField("widget",widgetEnabled.asJson))
  )
  def lookupTextField(widgetEnabled: Seq[String] = Seq()) = JSONField(JSONFieldTypes.STRING,"lookupTextField",true,label = Some("Dynamic label"), tooltip = Some("It can be a lookup or another field in the same form"), widget = Some(WidgetsNames.input),
    condition = Some(ConditionalField("widget",widgetEnabled.asJson))
  )


  val formName = JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.input))
  val formProps = JSONField(JSONFieldTypes.STRING,"props",true,label = Some("Props"), tooltip = Some("Comma separed list of fields that are extracted from parent form, it may be useful for conditional fields"), widget = Some(WidgetsNames.hidden))
  val formDescription =JSONField(JSONFieldTypes.STRING,"description",true,widget = Some(WidgetsNames.textarea))
  val formLayout = JSONField(JSONFieldTypes.JSON,"layout",true, widget = Some(WidgetsNames.popupWidget),label = Some("Layout"),
    params = Some(Json.obj("widget" -> WidgetsNames.adminLayoutWidget.asJson,"language" -> "json".asJson, "height" -> 600.asJson))
  )

  val formParamsLayout = JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.adminFormParamsLayout))
  val params = JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code))

  val formFieldChild = JSONField(JSONFieldTypes.CHILD,"fields_child",true,
    child = Some(Child(FORM_FIELD_CHILDS,"fields_child",Seq("form_uuid"),Seq("form_uuid"),
      Some(JSONQuery.sortByKeys(Seq("field_uuid")).filterWith(JSONQueryFilter.WHERE.eq("type",JSONFieldTypes.CHILD))),
      "",
      true
    )),
    widget = Some(WidgetsNames.tableChild),
    params = Some(Json.fromFields(Map(
      "props" -> Json.fromFields(Map(
        "entity" -> s"${Widget.REF}entity".asJson
      ))
    )))
  )
  val formFieldStatic = JSONField(JSONFieldTypes.CHILD,"fields_static",true,
    child = Some(Child(FORM_FIELD_STATIC,"fields_static",Seq("form_uuid"),Seq("form_uuid"),
      Some(JSONQuery.sortByKeys(Seq("field_uuid")).filterWith(JSONQueryFilter.WHERE.eq("type",JSONFieldTypes.STATIC))),
      "",true
    )),
    widget = Some(WidgetsNames.tableChild)
  )

  val formi18n = JSONField(JSONFieldTypes.CHILD,"form_i18n",true,
    child = Some(Child(FORM_I18N,"form_i18n",Seq("form_uuid"),Seq("form_uuid"),Some(JSONQuery.sortByKeys(Seq("lang"))),"",true)),
    widget = Some(WidgetsNames.tableChild)
  )


  val fieldi18n = JSONField(
    JSONFieldTypes.CHILD,
    "field_i18n",
    true,
    child = Some(Child(FORM_FIELD_I18N,"field_i18n",Seq("field_uuid"),Seq("field_uuid"),Some(JSONQuery.sortByKeys(Seq("lang"))),"",true)),
    widget = Some(WidgetsNames.tableChild),
    params = Some(Json.fromFields(Map(
      "props" -> Json.fromFields(Map(
        "widget" -> s"${Widget.REF}widget".asJson,
        "foreign_entity" -> s"${Widget.REF}foreign_entity".asJson
      ))
    )))
  )


}
