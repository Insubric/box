package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.vendors.speech.BiasMethod.Grammar
import ch.wsl.box.client.vendors.speech.{SpeechRecognition, WebSpeech}
import ch.wsl.box.client.utils.StringUtils
import ch.wsl.box.model.shared.{JSONField, JSONLookup, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

case class VoiceInsert(field:JSONField,value:Json)

class VoiceHelper(metadata:JSONMetadata)(implicit ex:ExecutionContext) extends Logging {

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

    Lookup.fetchLookup(metadata,field,false,force = true).flatMap {
      case Seq() => rawFieldInsert(field)
      case lookups => lookupFieldInsert(field,lookups)
    }


  }

  private def lookupFieldInsert(field:JSONField,lookups:Seq[JSONLookup]):Future[VoiceInsert] = {
    logger.debug(s"Looking in $lookups")
    rec.listenOnce().flatMap { r =>
      val similarities = r.sortBy(_.confidence).map { x =>
        lookups.map(l => (l, StringUtils.stringSimilarity(x.transcript.toLowerCase(), l.value.toLowerCase()))).maxBy(_._2)
      }
      logger.debug(similarities.toList.toString())
      similarities.maxBy(_._2) match {
        case value if value._2 > 0.8 => synthesis.speak(
          lang = "it-IT",
          text = value._1.values.mkString(" "),
          rate = 1.2
        ).map(_ => VoiceInsert(field, value._1.id))
        case _ => synthesis.speak(
          lang = "it-IT",
          text = "Non riconosciuto, riprova",
          rate = 1.2
        ).flatMap { _ =>
          lookupFieldInsert(field,lookups)
        }
      }
    }
  }


  private def rawFieldInsert(field: JSONField): Future[VoiceInsert] = {
    rec.listenOnce().flatMap { r =>
      r.sortBy(-_.confidence).flatMap(x => JSONUtils.toJs(x.transcript, field)).headOption match {
        case Some(value) => synthesis.speak(
          lang = "it-IT",
          text = value.string,
          rate = 1.2
        ).map(_ => VoiceInsert(field, value))
        case None => synthesis.speak(
          lang = "it-IT",
          text = "Non riconosciuto, riprova",
          rate = 1.2
        ).flatMap { _ =>
          rawFieldInsert(field)
        }
      }
    }
  }

}
