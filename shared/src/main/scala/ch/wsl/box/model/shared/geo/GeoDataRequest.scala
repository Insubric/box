package ch.wsl.box.model.shared.geo

import ch.wsl.box.model.shared.JSONQuery

case class GeoDataRequest(query:JSONQuery,properties:Seq[String])
