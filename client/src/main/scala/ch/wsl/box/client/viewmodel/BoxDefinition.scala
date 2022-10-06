package ch.wsl.box.client.viewmodel

import io.circe.Json
import io.udash.Blank
import io.udash.properties.HasModelPropertyCreator

case class BoxDefinition(
                          cron: Seq[Json] = Seq(),
                          export: Seq[Json] = Seq(),
                          export_i18n: Seq[Json] = Seq(),
                          export_field: Seq[Json] = Seq(),
                          export_field_i18n: Seq[Json] = Seq(),
                          form: Seq[Json] = Seq(),
                          form_i18n: Seq[Json] = Seq(),
                          form_actions: Seq[Json] = Seq(),
                          field: Seq[Json] = Seq(),
                          field_i18n: Seq[Json] = Seq(),
                          function: Seq[Json] = Seq(),
                          function_i18n: Seq[Json] = Seq(),
                          function_field: Seq[Json] = Seq(),
                          function_field_i18n: Seq[Json] = Seq(),
                          labels: Seq[Json] = Seq(),
                          news: Seq[Json] = Seq(),
                          news_i18n: Seq[Json] = Seq(),
//                          ui: Seq[Json] = Seq(),
//                          ui_src: Seq[Json] = Seq(),
                        )


case class MergeElement[T](insert:Seq[T] = Seq(),delete:Seq[T] = Seq(),update:Seq[T] = Seq(),toUpdate:Option[Seq[T]] = None)

object MergeElement{
  def empty[T]() = MergeElement[T]()
}

object BoxDef {

  sealed trait Mode
  case object Insert extends Mode
  case object Update extends Mode
  case object Delete extends Mode

  type BoxDefinitionMerge = Map[String, MergeElement[Json]]

  def empty:BoxDefinitionMerge = Map(
    "cron" ->  MergeElement.empty(),
    "export" ->  MergeElement.empty(),
    "export_i18n" ->  MergeElement.empty(),
    "export_field" ->  MergeElement.empty(),
    "export_field_i18n" ->  MergeElement.empty(),
    "form" ->  MergeElement.empty(),
    "form_i18n" ->  MergeElement.empty(),
    "form_actions" ->  MergeElement.empty(),
    "field" ->  MergeElement.empty(),
    "field_i18n" ->  MergeElement.empty(),
    "function" ->  MergeElement.empty(),
    "function_i18n" ->  MergeElement.empty(),
    "function_field" ->  MergeElement.empty(),
    "function_field_i18n" ->  MergeElement.empty(),
    "labels" ->  MergeElement.empty(),
    "news" ->  MergeElement.empty(),
    "news_i18n" ->  MergeElement.empty(),
//    "ui" ->  MergeElement.empty(),
//    "ui_src" ->  MergeElement.empty(),
  )

}

//case class BoxDefinitionMerge(
//                               //access_levels:MergeElement[BoxAccessLevel.BoxAccessLevel_row],
//                               //conf: MergeElement[BoxConf.BoxConf_row],
//                               cron: MergeElement[Json] = MergeElement.empty(),
//                               export: MergeElement[Json] = MergeElement.empty(),
//                               export_i18n: MergeElement[Json] = MergeElement.empty(),
//                               export_field: MergeElement[Json] = MergeElement.empty(),
//                               export_field_i18n: MergeElement[Json] = MergeElement.empty(),
//                               export_header_i18n: MergeElement[Json] = MergeElement.empty(),
//                               form: MergeElement[Json] = MergeElement.empty(),
//                               form_i18n: MergeElement[Json] = MergeElement.empty(),
//                               form_actions: MergeElement[Json] = MergeElement.empty(),
//                               field: MergeElement[Json] = MergeElement.empty(),
//                               field_file: MergeElement[Json] = MergeElement.empty(),
//                               field_i18n: MergeElement[Json] = MergeElement.empty(),
//                               function: MergeElement[Json] = MergeElement.empty(),
//                               function_i18n: MergeElement[Json] = MergeElement.empty(),
//                               function_field: MergeElement[Json] = MergeElement.empty(),
//                               function_field_i18n: MergeElement[Json] = MergeElement.empty(),
//                               labels: MergeElement[Json] = MergeElement.empty(),
//                               news: MergeElement[Json] = MergeElement.empty(),
//                               news_i18n: MergeElement[Json] = MergeElement.empty(),
//                               ui: MergeElement[Json] = MergeElement.empty(),
//                               ui_src: MergeElement[Json] = MergeElement.empty(),
//                               //users: MergeElement[BoxUser.BoxUser_row]
//                             )

