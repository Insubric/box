package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared.{Child, EntityKind, FormActionsMetadata, JSONField, JSONFieldTypes, JSONMetadata, JSONQuery, JSONSort, Layout, LayoutBlock, NaturalKey, Sort, SubLayoutBlock, SurrugateKey, WidgetsNames}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.metadata.box.Constants.{LABEL, LABEL_CONTAINER, PAGE}

object LabelUIDef {

  val labelContainer = JSONMetadata(
    objId = LABEL_CONTAINER,
    kind = EntityKind.BOX_FORM.kind,
    name = "labels",
    label = "Labels",
    fields = Seq(
      JSONField(JSONFieldTypes.CHILD,"labels",false,
        widget = Some(WidgetsNames.editableTable),
        child = Some(Child(
          objId = LABEL,
          key = "labels",
          mapping = Seq(),
          childQuery = None,
          props = Seq(),
          true
        ))
      )
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,None,None,Seq("labels").map(Left(_))),
      )
    ),
    entity = FormMetadataFactory.STATIC_PAGE,
    lang = "en",
    tabularFields = Seq(),
    rawTabularFields = Seq(),
    keys = Seq(),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.saveOnly(false),
    static = true
  )

  def label(langs:Seq[String]) = JSONMetadata(
    objId = LABEL,
    kind = EntityKind.BOX_FORM.kind,
    name = "label",
    label = "Labels",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"key",false,widget = Some(WidgetsNames.input)),
    ) ++ langs.map{l =>
      JSONField(JSONFieldTypes.STRING,l,true,widget = Some(WidgetsNames.input))
    },
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,None,None,(Seq("key")++langs).map(Left(_))),
      )
    ),
    entity = "v_labels",
    lang = "en",
    tabularFields = Seq("key")++langs,
    rawTabularFields = Seq("key")++langs,
    keys = Seq("key"),
    keyStrategy = NaturalKey,
    query = Some(
      JSONQuery.limit(5000).sortWith(JSONSort("key",Sort.ASC))
    ),
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )
}
