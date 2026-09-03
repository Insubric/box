package ch.wsl.box.rest.auth.oidc

import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.model.boxentities.BoxOIDC.{BoxOIDCTable, BoxOIDC_row}
import ch.wsl.box.model.shared.oidc.OIDCCodeChallenge
import ch.wsl.box.jdbc.PostgresProfile.api._

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}




object CodeHandler {

  private val random = new SecureRandom()

  private val alphabet =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

  private def createCodeVerifier(length: Int = 128): String = Array.fill(length) {
    alphabet.charAt(random.nextInt(alphabet.length))
  }.mkString

  private def createCodeChallenge(codeVerifier: String): String = {
    val digest =
      MessageDigest.getInstance("SHA-256")
        .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII))

    Base64.getUrlEncoder
      .withoutPadding()
      .encodeToString(digest)
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

    val verifier = createCodeVerifier()

    val occ = OIDCCodeChallenge(
      state = UUID.randomUUID().toString,
      challenge = createCodeChallenge(verifier)
    )

    val oidc = BoxOIDC_row(occ.state,occ.challenge,verifier)

    db.run{
      for{
        _ <- BoxOIDCTable += oidc
      } yield occ
    }

  }
}
