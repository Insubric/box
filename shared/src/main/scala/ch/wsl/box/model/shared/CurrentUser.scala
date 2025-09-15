package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.oidc.UserInfo

case class DbInfo(username:String, app_username:String,roles:Seq[String])

object DbInfo {
  def simple(username:String) = DbInfo(username,username,Seq())
}

case class CurrentUser(db:DbInfo,profile:UserInfo)

object CurrentUser {
  def simple(username:String) = CurrentUser(DbInfo.simple(username),UserInfo.simple(username))
}
