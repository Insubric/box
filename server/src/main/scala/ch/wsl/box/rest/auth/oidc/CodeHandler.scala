package ch.wsl.box.rest.auth.oidc

import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.model.boxentities.BoxOIDC.{BoxOIDCTable, BoxOIDC_row}
import ch.wsl.box.model.shared.oidc.OIDCCodeChallenge
import ch.wsl.box.jdbc.PostgresProfile.api._

import java.security.MessageDigest
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}




object CodeHandler {

  def sha256Base64(input: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(hashBytes)
  }


  def verifierFromState(state:String,db:UserDatabase)(implicit ex:ExecutionContext):Future[String] = {
    db.run {
      for {
        verifier <- BoxOIDCTable.filter(_.state === state).map(_.verifier).result.head
        _ <- BoxOIDCTable.filter(_.state === state).delete
      } yield verifier
    }
  }

  def issueNewCode(db:UserDatabase)(implicit ex:ExecutionContext):Future[OIDCCodeChallenge] = {
    val verifier = UUID.randomUUID().toString ++ UUID.randomUUID().toString
    val occ = OIDCCodeChallenge(
      state = UUID.randomUUID().toString,
      challenge = sha256Base64(verifier)
    )

    val oidc = BoxOIDC_row(occ.state,occ.challenge,verifier)

    db.run{
      for{
        _ <- BoxOIDCTable += oidc
      } yield occ
    }

  }
}
