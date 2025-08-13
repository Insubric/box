package ch.wsl.box.rest.auth.oidc

import ch.wsl.box.model.shared.oidc.OIDCFrontendConf

case class OIDCConf(
                     provider_id:String,
                     name: String,
                     logo: String,
                     authorize_url: String,
                     token_url: String,
                     user_info_url: String,
                     scope: String,
                     client_id: String,
                     client_secret: String
                   ) {
  def toFrontend = OIDCFrontendConf(
    provider_id,
    name,
    logo,
    authorize_url,
    scope,
    client_id
  )
}

