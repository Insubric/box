package ch.wsl.box.model.shared

import io.circe.Json

case class JsonDiffField(
                          changedModel: String,
                          field:Option[String],
                          id:Option[JSONID],
                          old:Option[Json],
                          value:Option[Json],
                          insert: Boolean = false,
                          delete: Boolean = false
                        )

case class JsonDiff(fields:Seq[JsonDiffField])

