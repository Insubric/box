package ch.wsl.box.services.user_preferences

import io.circe.Json

import scala.concurrent.Future

trait UserPreference {

  def get(username:String):Future[Option[Json]]
  def set(username:String,data:Json):Future[Boolean]

}
