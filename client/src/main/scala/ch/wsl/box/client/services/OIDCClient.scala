package ch.wsl.box.client.services

import ch.wsl.box.model.shared.oidc.OIDCFrontendConf

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

trait OIDCClient {
  def login(provider:OIDCFrontendConf)(implicit ex:ExecutionContext):Unit
}
