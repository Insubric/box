package ch.wsl.box.model.shared.oidc

case class UserInfo(
                     sub:String,
                     preferred_username:String,
                     email:Option[String]
                   )