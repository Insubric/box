package ch.wsl.box.rest.auth.oidc

import io.circe._
import io.circe.generic.auto._
import sttp.client4.DefaultFutureBackend
import sttp.client4._
import sttp.client4.circe.asJson
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtOptions}

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class JwtValidator(jwksUrl: String, issuer: String)(implicit val ec: ExecutionContext) {

  case class JWK(kid: String, kty: String, alg: Option[String], n: String, e: String)
  case class JWKSet(keys: List[JWK])

  val backend = DefaultFutureBackend()

  /**
   * Fetches and parses the JWKS payload into typed case classes using sttp-circe.
   */
  private def fetchJwks(): Future[JWKSet] = {
    basicRequest
      .get(uri"$jwksUrl")
      .response(asJson[JWKSet]) // Automatically parses JSON with Circe
      .send(backend)
      .flatMap { response =>
        response.body match {
          case Right(jwkSet) => Future.successful(jwkSet)
          case Left(error)   => Future.failed(new Exception(s"JWKS fetch failed: ${error.getMessage}"))
        }
      }
  }


  private def decodeToBigInteger(base64UrlStr: String): BigInteger = {
    val decoder = Base64.getUrlDecoder
    new BigInteger(1, decoder.decode(base64UrlStr))
  }

  /**
   * Reconstructs an RSAPublicKey using the Modulus (n) and Exponent (e) fields.
   */
  private def buildPublicKey(jwk: JWK): RSAPublicKey = {
    val modulus  = decodeToBigInteger(jwk.n)
    val exponent = decodeToBigInteger(jwk.e)

    val spec       = new RSAPublicKeySpec(modulus, exponent)
    val factory    = KeyFactory.getInstance("RSA")
    factory.generatePublic(spec).asInstanceOf[RSAPublicKey]
  }


  /**
   * Validates an incoming token using Akka futures and Circe JWT.
   */
  def validateToken(token: String): Future[Boolean] = {
    // Read header unsafely first to extract 'kid' (Key ID)
    JwtCirce.decodeAll(token,JwtOptions.DEFAULT.copy(signature = false)) match {
      case Success((header,claim,_)) if header.keyId.isDefined =>
        val kid = header.keyId.get

        // Fetch current public keys from your IDP
        fetchJwks().flatMap { jwkSet =>
          // Find a key that matches the token's kid
          jwkSet.keys.find(_.kid == kid) match {
            case Some(matchingJwk) =>


              val publicKey: RSAPublicKey = buildPublicKey(matchingJwk)

              JwtCirce.decode(token, publicKey, Seq(JwtAlgorithm.RS256)) match {
                case Success(claim) if claim.issuer.contains(issuer) =>
                  Future.successful(true)
                case Success(_) =>
                  Future.failed(new Exception("Invalid token issuer"))
                case Failure(err) =>
                  Future.failed(new Exception(s"Signature validation failed: ${err.getMessage}"))
              }

            case None =>
              Future.failed(new Exception(s"No matching public key found for kid: $kid"))
          }
        }

      case _ =>
        Future.failed(new Exception("Invalid JWT header format or missing 'kid'"))
    }
  }

}
