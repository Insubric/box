package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared.{Child, ConditionalField, JSONField, JSONFieldLookup, JSONFieldTypes, JSONLookup, JSONQuery, JSONQueryFilter, WidgetsNames}
import ch.wsl.box.rest.metadata.box.Constants.{FORM_FIELD_CHILDS, FORM_FIELD_FILE, FORM_FIELD_I18N, FORM_FIELD_STATIC, FORM_I18N}
import io.circe.Json
import io.circe.syntax._

object CommonField {


  val name = JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.input))

  val widget = JSONField(JSONFieldTypes.STRING,"widget",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.withExtractor(
      "type",
      WidgetsNames.mapping.map{ case (k,v) => k.asJson -> v.map(x => JSONLookup(x.asJson,x))}
    )
  ))

  def typ(child:Boolean = true, static:Boolean = true) = JSONField(JSONFieldTypes.STRING,"type",false,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      JSONFieldTypes.ALL
        .filter(x => child || x != JSONFieldTypes.CHILD)
        .filter(x => static || x != JSONFieldTypes.STATIC)
        .sorted.map(x => JSONLookup(x.asJson,x))
    )
  ))

  def lookupEntity(tables:Seq[String]) =  JSONField(JSONFieldTypes.STRING,"lookupEntity",true,
    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      tables.map(x => JSONLookup(x.asJson,x))
    )),
    condition = Some(ConditionalField("widget",Seq(WidgetsNames.select.asJson,WidgetsNames.popup.asJson,WidgetsNames.lookupLabel.asJson)))
  )

  def lookupValueField(tables:Seq[String]) =  JSONField(JSONFieldTypes.STRING,"lookupValueField",true,
    condition = Some(ConditionalField("lookupEntity",tables.map(_.asJson))),
    widget = Some(WidgetsNames.input)
  )

  def lookupQuery(tables:Seq[String]) = JSONField(JSONFieldTypes.STRING,"lookupQuery",true,
    widget = Some(WidgetsNames.code),
    condition = Some(ConditionalField("lookupEntity",tables.map(_.asJson))),
    params = Some(Json.obj("language" -> "json".asJson, "height" -> 100.asJson, "fullWidth" -> false.asJson))
  )

  val default = JSONField(JSONFieldTypes.STRING,"default",true,widget = Some(WidgetsNames.input))

  val conditionFieldId = JSONField(JSONFieldTypes.STRING,"conditionFieldId",true,widget = Some(WidgetsNames.input))
  val conditionValues = JSONField(JSONFieldTypes.STRING,"conditionValues",true,
    widget = Some(WidgetsNames.code),
    placeholder = Some("[1,2,3]"),
    tooltip = Some("Enter a JSON array with the possibles values"),
    params = Some(Json.obj("language" -> "json".asJson, "height" -> 50.asJson, "fullWidth" -> false.asJson))
  )

  def lang(langs:Seq[String]) = JSONField(JSONFieldTypes.STRING,"lang",false,

    widget = Some(WidgetsNames.select),
    lookup = Some(JSONFieldLookup.prefilled(
      langs.map(x => JSONLookup(x.asJson,x))
    ))
  )


  val simpleLabel = JSONField(JSONFieldTypes.STRING,"label",true, widget = Some(WidgetsNames.input))

  def label(widgetDisabled:Seq[String] = Seq()) = JSONField(JSONFieldTypes.STRING,"label",true, widget = Some(WidgetsNames.dynamicWidget),
    condition = Some(ConditionalField("widget",WidgetsNames.all.diff(widgetDisabled).map(_.asJson))),
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
    condition = Some(ConditionalField("widget",widgetEnabled.map(_.asJson)))
  )
  def lookupTextField(widgetEnabled: Seq[String] = Seq()) = JSONField(JSONFieldTypes.STRING,"lookupTextField",true,label = Some("Dynamic label"), tooltip = Some("It can be a lookup or another field in the same form"), widget = Some(WidgetsNames.input),
    condition = Some(ConditionalField("widget",widgetEnabled.map(_.asJson)))
  )


  val formName = JSONField(JSONFieldTypes.STRING,"name",false,widget = Some(WidgetsNames.input))
  val formProps = JSONField(JSONFieldTypes.STRING,"props",true,label = Some("Props"), tooltip = Some("Comma separed list of fields that are extracted from parent form, it may be useful for conditional fields"), widget = Some(WidgetsNames.input))
  val formDescription =JSONField(JSONFieldTypes.STRING,"description",true,widget = Some(WidgetsNames.twoLines))
  val formLayout = JSONField(JSONFieldTypes.STRING,"layout",true, widget = Some(WidgetsNames.code),label = Some(""),
    params = Some(Json.obj("language" -> "json".asJson, "height" -> 600.asJson))
  )

  val params = JSONField(JSONFieldTypes.JSON,"params",true,widget = Some(WidgetsNames.code))

  val formFieldChild = JSONField(JSONFieldTypes.CHILD,"fields_child",true,
    child = Some(Child(FORM_FIELD_CHILDS,"fields_child","form_uuid","form_uuid",
      Some(JSONQuery.sortByKeys(Seq("field_uuid")).filterWith(JSONQueryFilter.WHERE.eq("type",JSONFieldTypes.CHILD))),
      ""
    )),
    widget = Some(WidgetsNames.tableChild)
  )
  val formFieldStatic = JSONField(JSONFieldTypes.CHILD,"fields_static",true,
    child = Some(Child(FORM_FIELD_STATIC,"fields_static","form_uuid","form_uuid",
      Some(JSONQuery.sortByKeys(Seq("field_uuid")).filterWith(JSONQueryFilter.WHERE.eq("type",JSONFieldTypes.STATIC))),
      ""
    )),
    widget = Some(WidgetsNames.tableChild)
  )

  val formi18n = JSONField(JSONFieldTypes.CHILD,"form_i18n",true,
    child = Some(Child(FORM_I18N,"form_i18n","form_uuid","form_uuid",Some(JSONQuery.sortByKeys(Seq("lang"))),"")),
    widget = Some(WidgetsNames.tableChild)
  )



}
