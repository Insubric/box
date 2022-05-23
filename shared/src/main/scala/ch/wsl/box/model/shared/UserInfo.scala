package ch.wsl.box.model.shared

case class UserInfo(
                     preferred_username:String,
                     email_verified:Boolean,
                     email:Option[String]
                   )