package ch.wsl.box.model.shared.oidc

case class OIDCFrontendConf(
                             provider_id:String,
                             name: String,
                             logo: String,
                             authorize_url: String,
                             scope: String,
                             client_id: String
                   )

object OIDCFrontendConf {
  val name = "openid_providers"
}