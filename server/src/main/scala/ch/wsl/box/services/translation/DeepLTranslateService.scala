package ch.wsl.box.services.translation

import ch.wsl.box.rest.auth.oidc.AuthFlow.OpenIDToken
import ch.wsl.box.services.config.{DeepLConfig, FullConfig}
import sttp.client4._
import sttp.client4.circe.asJson
import sttp.client4.{DefaultFutureBackend, basicRequest}
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContext, Future}



class DeepLTranslateService(conf: DeepLConfig)(implicit ex:ExecutionContext) extends TranslateService {

  private val apiKey = conf.deeplKey
  private val apiEndpoint = conf.deeplEndpoint

  private val backend = DefaultFutureBackend()


  private case class DeepLAPIRequest(
                                      text:Seq[String],
                                      target_lang: String,
                                      source_lang: String,
                                      context:Option[String]
                                    )



  private case class DeepLAPITranslations(
                                      text:String
                                    )

  private case class DeepLAPIResponse(
                                     translations: Seq[DeepLAPITranslations]
                                     )

  implicit private val reqEncoder = deriveEncoder[DeepLAPIRequest]
  implicit private val resTDecoder = deriveDecoder[DeepLAPITranslations]
  implicit private val resDecoder = deriveDecoder[DeepLAPIResponse]

  override def translate(from: String, to: String,text: String): Future[String] = translateAll(from,to,Seq(text)).map(_.head)

  override def translateAll(from: String, to: String,texts: Seq[String]): Future[Seq[String]] = {

    val translator = translatorBulk(from,to) _

    val blocks = texts.grouped(50)

    blocks.foldLeft(Future.successful(Seq[String]())) { (accF, text) =>
      for {
        acc <- accF
        res <- translator(text)
        throttle <- Future{ Thread.sleep(300) }
      } yield acc ++ res
    }
  }

  private def translatorBulk(from: String, to: String)(texts: Seq[String]): Future[Seq[String]] = {
    (apiKey,apiEndpoint) match {
      case (Some(key),Some(endpoint)) => {
        val r = basicRequest
          .post(uri"${endpoint}/v2/translate")
          .header("Authorization",s"DeepL-Auth-Key $key")
          .body(asJson(DeepLAPIRequest(texts,to.toUpperCase,from.toUpperCase,Some("Text is used in a web application that expose database tables on the web."))))
          .response(asJson[DeepLAPIResponse])

        print(r.toCurl)

        r.send(backend).map(_.body match {
          case Right(value) => value.translations.map(_.text)
          case Left(value) => throw value
        })
      }
      case (None,Some(_)) => throw new Exception("DeepL API Key not found")
      case (Some(_),None) => throw new Exception("DeepL Endpoint not found")
      case (None,None) => throw new Exception("DeepL API Key and endpoint not found")
    }
  }

}
