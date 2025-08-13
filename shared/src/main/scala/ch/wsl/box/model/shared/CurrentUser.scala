package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.oidc.UserInfo

case class DbInfo(username:String,roles:Seq[String])

case class CurrentUser(db:DbInfo,profile:UserInfo)
