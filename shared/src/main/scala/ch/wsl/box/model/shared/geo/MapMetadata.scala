package ch.wsl.box.model.shared.geo


import ch.wsl.box.model.shared.JSONQuery

import java.util.UUID

case class Box2d(
                  xMin: Double,
                  yMin: Double,
                  xMax: Double,
                  yMax: Double
                )



case class MapMetadata(
                        id: UUID,
                        name: String,
                        parameters: Seq[String],
                        srid: Option[Int],
                        boundingBox: Option[Box2d],
                        layers: Seq[MapLayerMetadata]
                      )
