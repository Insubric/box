package ch.wsl.box.model.shared.geo

import ch.wsl.box.model.shared.GeoJson.CRS

case class MapProjection(
                                name:String,
                                proj:String,
                                extent: Option[Seq[Double]] = None
                              ) {
  def crs = CRS(name)
}