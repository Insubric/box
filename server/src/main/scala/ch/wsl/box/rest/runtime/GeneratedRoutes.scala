package ch.wsl.box.rest.runtime

import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services

import scala.concurrent.ExecutionContext

trait GeneratedRoutes {
  def apply(lang: String)(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services):Route
}

trait GeneratedFileRoutes {
  def routeForField(field:String)(implicit up:UserProfile, materializer:Materializer, ec:ExecutionContext, services: Services):Route
  def apply()(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext, services: Services):Route
}