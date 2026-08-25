package ch.wsl.box.client.services.impl

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, HttpClient, OIDCClient}
import ch.wsl.box.model.shared.oidc.{OIDCCodeChallenge, OIDCFrontendConf}
import org.scalajs.dom.window

import java.net.URLEncoder
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import io.circe.generic.auto._

class OIDCClientImpl(client:HttpClient) extends OIDCClient {





  override def login(provider:OIDCFrontendConf)(implicit ex:ExecutionContext): Unit = {


    val redirectUri = URLEncoder.encode(s"${ClientConf.frontendUrl}authenticate/${provider.provider_id}","UTF-8")

    val baseRedirect = s"${provider.authorize_url}?client_id=${provider.client_id}&scope=${provider.scope}&response_type=code&redirect_uri=${redirectUri}"

    if(provider.code_challange) {
      client.get[OIDCCodeChallenge](Routes.apiV1("/sso/cognito/challenge")).foreach { c =>
        window.location.href = s"$baseRedirect&code_challenge=${c.challenge.substring(0, c.challenge.length - 1)}&code_challenge_method=S256&state=${c.state}"
      }
    } else window.location.href = baseRedirect+"&state="+UUID.randomUUID().toString
  }

}
