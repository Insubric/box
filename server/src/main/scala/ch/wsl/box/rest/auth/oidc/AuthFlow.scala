package ch.wsl.box.rest.auth.oidc

import ch.wsl.box.model.shared.{CurrentUser, DbInfo}
import ch.wsl.box.model.shared.oidc.UserInfo
import ch.wsl.box.rest.utils.Auth
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import sttp.client4._
import sttp.client4.circe.asJson
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

object AuthFlow {



  case class OpenIDToken(
                          access_token:String,
                          expires_in: Int,
                          refresh_expires_in: Option[Int],
                          refresh_token: Option[String],
                          token_type: String,
                          session_state: Option[String],
                          scope: String
                        )



  val backend = DefaultFutureBackend()


  private def currentUserFromUserInfo(provider:OIDCConf,userInfo: UserInfo)(implicit ec:ExecutionContext, services:Services):Future[CurrentUser] = {

    val dbUser = services.config.singleUser match {
      case true => services.connection.user
      case false => provider.db_role_claim.flatMap(c => userInfo.claims.getOpt(c))
        .getOrElse(userInfo.preferred_username)
    }

    val appUser = provider.app_user_claim.orElse(provider.db_role_claim).flatMap(c => userInfo.claims.getOpt(c))
      .getOrElse(userInfo.preferred_username)

    Auth.rolesOf(dbUser).map{ roles =>
      CurrentUser(DbInfo(dbUser,appUser,roles),userInfo.copy(roles = roles))
    }

  }

  //  curl --location --request POST 'http://localhost:8180/auth/realms/master/protocol/openid-connect/token' \
  //    --header 'Content-Type: application/x-www-form-urlencoded' \
  //  --data-urlencode 'grant_type=authorization_code' \
  //  --data-urlencode 'client_id=box' \
  //  --data-urlencode 'client_secret=ztoNKqtFUNvPbNIxTXk9kVWZR2nGtm9J' \
  //  --data-urlencode 'code=48289e84-578f-45a9-8c9f-bc49fe336e3b.57b36158-2ad7-4614-80bf-3aa037ab38fe.2d13c739-13d5-4655-877f-56944b38e55e' \
  //  --data-urlencode 'redirect_uri=http://localhost:8080/test-auth'
  //  {
  //    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJKb1V5am1QSWtPZndUNTBDalZ3Nk5iUDczYko0bTB1ODlvbjRPTHFZUndFIn0.eyJleHAiOjE2NTMwNTc3NDMsImlhdCI6MTY1MzA1NzY4MywiYXV0aF90aW1lIjoxNjUzMDU3NTk2LCJqdGkiOiI1NjUzN2JkYS03MjQ0LTQzNmEtYTFmYS05M2EzMWQxMDA5M2IiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiMmM3YTgzOTEtYTg4MS00MjRlLWE4ZDQtMTgxODI1ZjMwYmRkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYm94Iiwic2Vzc2lvbl9zdGF0ZSI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJhZG1pbiIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJzaWQiOiI1N2IzNjE1OC0yYWQ3LTQ2MTQtODBiZi0zYWEwMzdhYjM4ZmUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluIn0.bJzESYSvwZCCQ4J4oOHbLeVmutz6degqb-jgpEFRDxgcaqmOz3cEfezWcWf0ekJO2DGSiBYIojggvxXp-yktx4oZT3Px4ELXjGpYRqI-eEe6WIghDe2SoC1H1KHyyuRUwWF_Jv9rO-9UsBGXNzzoQFrC8WkhPYaQf6gHfwjSnrQImbPhPykTbP1cjjHNPJESFBv84dAbvB2zrHBq4SiPQVbAgfpu4c8sIl6U21tPswQCTaqL8FE9uOkpExMyX2B7weLgOy1kRLofIz6DydoOSExgeoWlOsco0w_ILdH1Ipimfal2Ptls5eOu2QbMfH7zgQKhd2la_djVCZF5gtyrfw",
  //    "expires_in": 60,
  //    "refresh_expires_in": 1800,
  //    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxNWM2YmQ5Ny00YzE5LTQ1ZWYtYWI2Zi1lMzQ3MzQxMThhZjAifQ.eyJleHAiOjE2NTMwNTk0ODMsImlhdCI6MTY1MzA1NzY4MywianRpIjoiMWVhNDZjOWYtNTFiYy00NjI3LWIyZTYtYTUzZjk1YzQyY2E2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MTgwL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODE4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJzdWIiOiIyYzdhODM5MS1hODgxLTQyNGUtYThkNC0xODE4MjVmMzBiZGQiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiYm94Iiwic2Vzc2lvbl9zdGF0ZSI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSIsInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSJ9.wZvA70cNQ1_w7UJVJCWbmoVkGYBJjR-Dc7-SnPDTDRs",
  //    "token_type": "Bearer",
  //    "not-before-policy": 0,
  //    "session_state": "57b36158-2ad7-4614-80bf-3aa037ab38fe",
  //    "scope": "email profile"
  //  }
  def code(provider:OIDCConf,c:String)(implicit ex:ExecutionContext, services: Services):Future[Either[ResponseException[String],CurrentUser]] = {

    def authToken = basicRequest
      .post(uri"${provider.token_url}")
      .body(Map(
        "grant_type" -> "authorization_code",
        "client_id" -> provider.client_id,
        "client_secret" -> provider.client_secret,
        "code" -> c,
        "redirect_uri" -> s"${services.config.frontendUrl}/authenticate/${provider.provider_id}"
      ))
      .response(asJson[OpenIDToken])
      .send(backend)

    def userInfo(token:OpenIDToken) = {
      import UserInfo._
      basicRequest
        .get(uri"https://gitlabext.wsl.ch/oauth/userinfo")
        .response(asJson[UserInfo])
        .auth.bearer(token.access_token)
        .send(backend)
    }

    for{
      token <- authToken
      info <- token.body match {
        case Left(value) => Future.successful(Left(value))
        case Right(value) => userInfo(value).map(_.body)
      }
      cu <- info match {
        case Left(value) => Future.successful(Left(value))
        case Right(value) => currentUserFromUserInfo(provider,value).map(x => Right(x))
      }
    } yield cu

  }


  def code(provider_id:String,c:String)(implicit ex:ExecutionContext, services: Services):Future[Either[ResponseException[String],CurrentUser]] = {

    services.config.openid.find(_.provider_id == provider_id) match {
      case Some(provider) => code(provider, c)
      case None => Future.failed(new Exception(s"OIDC Provider $provider_id not found"))
    }

  }






}
