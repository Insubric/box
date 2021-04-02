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
    name = "news",
    label = "News editor",
    fields = Seq(
      JSONField(JSONFieldTypes.NUMBER,"news_id",false),
      JSONField(JSONFieldTypes.DATETIME,"datetime",false, widget = Some(WidgetsNames.datetimePicker)),
      JSONField(JSONFieldTypes.STRING,"author",true, widget = Some(WidgetsNames.input)),
      JSONField(JSONFieldTypes.CHILD,"news_i18n",true,child = Some(Child(NEWS_I18N,"news_i18n","news_id","news_id",None,"")), widget = Some(WidgetsNames.tableChild))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,4, None, Seq("datetime","author").map(Left(_))),
        LayoutBlock(Some("Translations"),8, None,Seq("news_i18n").map(Left(_)))
      )
    ),
//    layout = parse("""
//        |""").toOption.flatMap(_.as[Layout].toOption).getOrElse(Layout(Seq())),
    entity = "news",
    lang = "it",
    tabularFields = Seq("news_id","datetime","author"),
    rawTabularFields = Seq("news_id","datetime","author"),
    keys = Seq("news_id"),
    keyStrategy = SurrugateKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

  val newsI18n = JSONMetadata(
    objId = NEWS_I18N,
    name = "newsI18n",
    label = "NewsI18n builder",
    fields = Seq(
      JSONField(JSONFieldTypes.NUMBER,"news_id",false),
      CommonField.lang,
      JSONField(JSONFieldTypes.STRING,"text",true, widget = Some(WidgetsNames.richTextEditor)),
      JSONField(JSONFieldTypes.STRING,"title",true, widget = Some(WidgetsNames.input))
    ),
    layout = Layout(
      blocks = Seq(
        LayoutBlock(None,3, None,Seq("lang").map(Left(_))),
        LayoutBlock(None,9, None,Seq("title","text").map(Left(_))),
      )
    ),
    entity = "news_i18n",
    lang = "en",
    tabularFields = Seq("news_id","lang","title","text"),
    rawTabularFields = Seq("news_id","lang","title","text"),
    keys = Seq("news_id","lang"),
    keyStrategy = NaturalKey,
    query = None,
    exportFields = Seq(),
    view = None,
    action = FormActionsMetadata.default
  )

}
