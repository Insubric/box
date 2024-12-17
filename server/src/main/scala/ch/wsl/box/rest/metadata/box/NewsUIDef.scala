package ch.wsl.box.rest.metadata.box

import ch.wsl.box.model.shared._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._


object NewsUIDef {

  import Constants._
  import io.circe._
  import io.circe.syntax._

  val main = JSONMetadata(
    objId = NEWS,
    kind = EntityKind.BOX_FORM.kind,
    name = "news",
    label = "News editor",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"news_uuid",false),
      JSONField(JSONFieldTypes.DATETIME,"datetime",false, widget = Some(WidgetsNames.datetimePicker)),
      JSONField(JSONFieldTypes.STRING,"author",true, widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.CHILD,"news_i18n",true,child = Some(Child(NEWS_I18N,"news_i18n",Seq("news_uuid"),Seq("news_uuid"),None,"",true)), widget = Some(WidgetsNames.tableChild))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,4,  Seq("datetime","author").map(Left(_))),
        LayoutBlock(Some(Left("Translations")),8, Seq("news_i18n").map(Left(_)))
      )
    ),
//    layout = parse("""
//        |""").toOption.flatMap(_.as[Layout].toOption).getOrElse(Layout(Seq())),
    entity = "news",
    lang = "it",
    tabularFields = Seq("news_uuid","datetime","author"),
    rawTabularFields = Seq("news_uuid","datetime","author"),
    keys = Seq("news_uuid"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  def newsI18n(langs:Seq[String]) = JSONMetadata(
    objId = NEWS_I18N,
    kind = EntityKind.BOX_FORM.kind,
    name = "newsI18n",
    label = "NewsI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.STRING,"news_uuid",false),
      CommonField.lang(langs),
      JSONField(JSONFieldTypes.STRING,"text",true, widget = Some(WidgetsNames.richTextEditor)),
      JSONField(JSONFieldTypes.STRING,"title",true, widget = Some(WidgetsNames.input))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,3, Seq("lang").map(Left(_))),
        LayoutBlock(None,9, Seq("title","text").map(Left(_))),
      )
    ),
    entity = "news_i18n",
    lang = "en",
    tabularFields = Seq("news_uuid","lang","title","text"),
    rawTabularFields = Seq("news_uuid","lang","title","text"),
    keys = Seq("news_uuid","lang"),
    keyStrategy = NaturalKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

}
