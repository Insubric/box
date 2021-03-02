package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared.{Child, FormActionsMetadata, JSONField, JSONFieldTypes, JSONMetadata, JSONQuery, JSONSort, Layout, LayoutBlock, NaturalKey, Sort, SubLayoutBlock, SurrugateKey, WidgetsNames}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.metadata.box.Constants.{LABEL, LABEL_CONTAINER, PAGE}
import ch.wsl.box.rest.utils.BoxConfig

object LabelUIDef {

  val labelContainer = JSONMetadata(
    objId = LABEL_CONTAINER,
    name = "labels",
    label = "Labels",
    fields = Seq(
      JSONField(JSONFieldTypes.CHILD,"labels",false,
        widget = Some(WidgetsNames.editableTable),
        child = Some(Child(
          objId = LABEL,
          key = "labels",
          mapping = Seq(),
          childQuery = None
        ))
      )
    ),
    Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,Seq("labels").map(Left(_))),
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
    action = FormActionsMetadata.saveOnly,
    static = true
  )

  val label = JSONMetadata(
    objId = LABEL,
    name = "label",
    label = "Labels",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"key",false,widget = Some(WidgetsNames.input)),
    ) ++ BoxConfig.langs.map{l =>
      JSONField(JSONFieldTypes.STRING,l,true,widget = Some(WidgetsNames.input))
    },
    Layout(
      blocks = Seq(
        LayoutBlock(None,12,None,(Seq("key")++BoxConfig.langs).map(Left(_))),
      )
    ),
    entity = "v_labels",
    lang = "en",
    tabularFields = Seq("key")++BoxConfig.langs,
    rawTabularFields = Seq("key")++BoxConfig.langs,
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
