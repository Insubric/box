package ch.wsl.box.model.shared

case class UserInfo(
                     sub:String,
                     preferred_username:String,
                     email_verified:Boolean,
                     email:Option[String]
                   )