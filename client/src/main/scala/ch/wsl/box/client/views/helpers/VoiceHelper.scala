package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.vendors.speech.BiasMethod.Grammar
import ch.wsl.box.client.vendors.speech.{SpeechRecognition, WebSpeech}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

case class VoiceInsert(field:JSONField,value:Json)

class VoiceHelper(metadata:JSONMetadata)(implicit ex:ExecutionContext) {

  val rec = WebSpeech.recognition(lang = "it-IT")
  rec.onStart(() => println("started"))
  rec.onEnd(() => println("end"))

  val synthesis = WebSpeech.synthesis

  def selectField():Future[VoiceInsert] = {

    //rec.setPhrases(metadata.fields.map(f => f.title -> 10),Grammar)


    rec.listenOnce().flatMap{ speech =>
      println(speech)


      metadata.fields.find(x => speech.exists(s => s.transcript.toLowerCase == x.title.toLowerCase)) match {
        case Some(f) => {
          synthesis.speak(
            lang = "it-IT",
            text = f.title,
            rate = 1.2
          ).flatMap{ _ =>
            insertValue(f)
          }
        }
        case None => {
          synthesis.speak(
            lang = "it-IT",
            text = "Non riconosciuto, riprova",
            rate = 1.2
          ).flatMap { _ =>
            selectField()
          }
        }
      }



    }.recover {
      case t:Throwable => t.printStackTrace()
      throw t
    }


  }

  def insertValue(field:JSONField):Future[VoiceInsert] = {
    println("Insert value")
    rec.listenOnce().flatMap { r =>
      r.sortBy(-_.confidence).flatMap(x => JSONUtils.toJs(x.transcript, field)).headOption match {
        case Some(value) => synthesis.speak(
          lang = "it-IT",
          text = value.string,
          rate = 1.2
        ).map(_ => VoiceInsert(field,value))
        case None => synthesis.speak(
          lang = "it-IT",
          text = "Non riconosciuto, riprova",
          rate = 1.2
        ).flatMap { _ =>
          insertValue(field)
        }
      }
    }
  }

}
