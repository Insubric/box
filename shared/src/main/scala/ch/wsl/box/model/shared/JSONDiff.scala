package ch.wsl.box.model.shared

import io.circe.Json

case class JSONDiffField(
                          field:Option[String],
                          old:Option[Json],
                          value:Option[Json],
                          insert: Boolean = false,
                          delete: Boolean = false
                        )

case class JSONDiffModel(model:String,id:Option[JSONID],fields:Seq[JSONDiffField])

case class JSONDiff(models:Seq[JSONDiffModel])

