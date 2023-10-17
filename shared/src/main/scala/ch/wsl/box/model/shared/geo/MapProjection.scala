package ch.wsl.box.model.shared.geo

case class MapProjection(
                                name:String,
                                proj:String,
                                extent: Option[Seq[Double]] = None
                              )