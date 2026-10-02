package ch.wsl.box.client.vendors

package speech

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.vendors.speech.BiasMethod.Phrases

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.util.Try

// ---------------------------------------------------------------------------
// Speech Recognition - native facades
// ---------------------------------------------------------------------------


sealed trait BiasMethod

object BiasMethod {
  case object Phrases extends BiasMethod
  case object Grammar extends BiasMethod
  case object Unsupported extends BiasMethod
}

@js.native
trait SpeechRecognition extends js.Object {

  var phrases: js.Array[SpeechRecognitionPhrase] = js.native

  var lang: String = js.native
  var continuous: Boolean = js.native
  var interimResults: Boolean = js.native
  var maxAlternatives: Int = js.native
  var quality: String = js.native

  def start(): Unit = js.native
  def stop(): Unit = js.native
  def abort(): Unit = js.native

  var onresult: js.Function1[SpeechRecognitionEvent, Unit] = js.native
  var onerror: js.Function1[SpeechRecognitionErrorEvent, Unit] = js.native

  var onstart: js.Function1[js.Any, Unit] = js.native
  var onend: js.Function1[js.Any, Unit] = js.native

  var grammars: SpeechGrammarList = js.native

  var onspeechstart: js.Function1[js.Any, Unit] = js.native
  var onspeechend: js.Function1[js.Any, Unit] = js.native
}

@js.native
trait SpeechRecognitionEvent extends js.Object {

  val resultIndex: Int = js.native

  val results: SpeechRecognitionResultList = js.native
}

@js.native
trait SpeechRecognitionResultList extends js.Object {

  val length: Int = js.native

  @JSBracketAccess
  def apply(index: Int): SpeechRecognitionResult = js.native
}

@js.native
trait SpeechRecognitionResult extends js.Object {

  val length: Int = js.native

  val isFinal: Boolean = js.native

  @JSBracketAccess
  def apply(index: Int): SpeechRecognitionAlternative = js.native
}

@js.native
trait SpeechRecognitionAlternative extends js.Object {

  val transcript: String = js.native

  val confidence: Double = js.native
}

@js.native
trait SpeechRecognitionErrorEvent extends js.Object {

  val error: String = js.native

  val message: String = js.native
}


// ---------------------------------------------------------------------------
// Speech Synthesis - native facades
// ---------------------------------------------------------------------------

@js.native
trait SpeechSynthesisApi extends js.Object {

  val pending: Boolean = js.native
  val speaking: Boolean = js.native
  val paused: Boolean = js.native

  def speak(utterance: SpeechSynthesisUtterance): Unit = js.native

  def cancel(): Unit = js.native

  def pause(): Unit = js.native

  def resume(): Unit = js.native

  def getVoices(): js.Array[SpeechSynthesisVoice] = js.native

  var onvoiceschanged: js.Function1[js.Any, Unit] = js.native
}

@js.native
@JSGlobal("speechSynthesis")
object SpeechSynthesis extends SpeechSynthesisApi

@js.native
@JSGlobal("SpeechSynthesisUtterance")
class SpeechSynthesisUtterance(val text: String = js.native) extends js.Object {

  var lang: String = js.native

  var voice: SpeechSynthesisVoice = js.native

  var volume: Double = js.native
  var rate: Double = js.native
  var pitch: Double = js.native

  var onstart: js.Function1[js.Any, Unit] = js.native
  var onend: js.Function1[js.Any, Unit] = js.native
  var onerror: js.Function1[SpeechSynthesisErrorEvent, Unit] = js.native
}

@js.native
trait SpeechSynthesisVoice extends js.Object {

  val default: Boolean = js.native

  val lang: String = js.native

  val localService: Boolean = js.native

  val name: String = js.native

  val voiceURI: String = js.native
}

@js.native
trait SpeechSynthesisErrorEvent extends js.Object {

  val error: String = js.native
}


// ---------------------------------------------------------------------------
// Scala API
// ---------------------------------------------------------------------------

final case class RecognitionAlternative(
                                         transcript: String,
                                         confidence: Double
                                       )


final case class SpeechRecognitionException(
                                             error: String,
                                             message: String
                                           ) extends RuntimeException(
  if (message.nonEmpty) s"$error: $message"
  else error
)


object WebSpeech {

  // -------------------------------------------------------------------------
  // Feature detection
  // -------------------------------------------------------------------------

  def phrasesSupported: Boolean = {

    val constructor =
      js.Dynamic.global.selectDynamic("SpeechRecognitionPhrase")

    !js.isUndefined(constructor) &&
      constructor != null
  }


  def grammarSupported: Boolean =
    grammarListConstructor.isDefined

  def recognitionSupported: Boolean =
    recognitionConstructor.isDefined

  def synthesisSupported: Boolean = {
    val synth = js.Dynamic.global.selectDynamic("speechSynthesis")

    !js.isUndefined(synth) && synth != null
  }

  private def recognitionConstructor: Option[js.Dynamic] = {

    val standard =
      js.Dynamic.global.selectDynamic("SpeechRecognition")

    if (
      !js.isUndefined(standard) &&
        js.typeOf(standard) == "function"
    ) {
      Some(standard)
    } else {
      val webkit =
        js.Dynamic.global.selectDynamic("webkitSpeechRecognition")

      if (
        !js.isUndefined(webkit) &&
          js.typeOf(webkit) == "function"
      )
        Some(webkit)
      else
        None
    }
  }



  private[speech] def grammarListConstructor: Option[js.Dynamic] = {

    val standard =
      js.Dynamic.global.selectDynamic("SpeechGrammarList")

    if (
      !js.isUndefined(standard) &&
        js.typeOf(standard) == "function"
    ) {
      Some(standard)
    } else {
      val webkit =
        js.Dynamic.global.selectDynamic("webkitSpeechGrammarList")

      if (
        !js.isUndefined(webkit) &&
          js.typeOf(webkit) == "function"
      )
        Some(webkit)
      else
        None
    }
  }

  // -------------------------------------------------------------------------
  // Recognition
  // -------------------------------------------------------------------------

  def recognition(
                   lang: String = "en-US",
                   continuous: Boolean = false,
                   interimResults: Boolean = false,
                   maxAlternatives: Int = 10
                 ): SpeechRecognizer = {

    val constructor =
      recognitionConstructor.getOrElse {
        throw new UnsupportedOperationException(
          "Speech recognition is not supported by this browser"
        )
      }

    val native =
      js.Dynamic
        .newInstance(constructor)()
        .asInstanceOf[SpeechRecognition]

    native.lang = lang
    native.continuous = continuous
    native.interimResults = interimResults
    native.maxAlternatives = maxAlternatives

    new SpeechRecognizer(native)
  }

  // -------------------------------------------------------------------------
  // Synthesis
  // -------------------------------------------------------------------------

  val synthesis: TextToSpeech =
    new TextToSpeech
}


// ---------------------------------------------------------------------------
// High-level Speech Recognition wrapper
// ---------------------------------------------------------------------------

final class SpeechRecognizer private[speech] (
                                               val underlying: SpeechRecognition
                                             ) {

  def start(): Unit =
    underlying.start()

  def stop(): Unit =
    underlying.stop()

  def abort(): Unit =
    underlying.abort()

  def onResult(
                callback: Seq[RecognitionAlternative] => Unit
              ): Unit = {

    underlying.onresult =
      (event: SpeechRecognitionEvent) => {
        callback(extract(event))
      }
  }


  def onError(
               callback: SpeechRecognitionException => Unit
             ): Unit = {

    underlying.onerror =
      (event: SpeechRecognitionErrorEvent) => {
        BrowserConsole.log(event)
        callback(
          SpeechRecognitionException(
            event.error,
            Option(event.message).getOrElse("")
          )
        )
      }
  }

  def onStart(callback: () => Unit): Unit =
    underlying.onstart =
      (_: js.Any) => callback()

  def onEnd(callback: () => Unit): Unit =
    underlying.onend =
      (_: js.Any) => callback()

  /**
   * Listen for one final recognition result.
   *
   * Recognition is stopped automatically after receiving it.
   */
  def listenOnce()(implicit ec:ExecutionContext): Future[Seq[RecognitionAlternative]] = {

    val promise =
      Promise[Seq[RecognitionAlternative]]()

    underlying.continuous = false
    underlying.interimResults = false
    underlying.quality = "command"


    BrowserConsole.log(underlying)

    underlying.onresult =
      (event: SpeechRecognitionEvent) => {
        BrowserConsole.log(event)
        val results: Seq[RecognitionAlternative] = extract(event)
        println(results)
        if(results.nonEmpty) {
          promise.trySuccess(results)
          underlying.stop()
        }

      }


    underlying.onerror =
      (event: SpeechRecognitionErrorEvent) => {
        BrowserConsole.log(event)
        promise.tryFailure(
          SpeechRecognitionException(
            event.error,
            Option(event.message).getOrElse("")
          )
        )
      }

    println("Starting..")
    Future {
      Try(underlying.start()).failed.foreach {
        promise.tryFailure
      }
    }

    promise.future
  }

  private def extract(
                       event: SpeechRecognitionEvent
                     ): Seq[RecognitionAlternative] = {

    val results = for{
      i <- 0 until event.results.length
    } yield event.results.apply(i)

    results.filter(_.isFinal).flatMap { r =>
      for {
        i <- 0 until r.length
      } yield r.apply(i)
    }.map{ r =>
      RecognitionAlternative(
        r.transcript,
        r.confidence
      )
    }.toList


  }



  def setPhrases(
                  phrases: Seq[(String, Double)],
                  mode:BiasMethod
                ) = {

    if(mode == Phrases)
      setNativePhrases(phrases)
    else
      setGrammarFallback(phrases)
  }


  private def setNativePhrases(
                                phrases: Seq[(String, Double)]
                              ): Unit = {

    underlying.phrases =
      js.Array(
        phrases.map { case (phrase, boost) =>
          new SpeechRecognitionPhrase(
            phrase,
            boost.max(0.0).min(10.0)
          )
        }:_*
      )
  }

  private def setGrammarFallback(
                                  phrases: Seq[(String, Double)]
                                ): BiasMethod = {

    WebSpeech.grammarListConstructor match {

      case Some(constructor) =>

        val grammarList =
          js.Dynamic
            .newInstance(constructor)()
            .asInstanceOf[SpeechGrammarList]

        val grammar =
          createGrammar(
            phrases.map(_._1)
          )


        println(grammar)

        // JSGF only has one weight here, so use the
        // average boost as an approximation.
        val weight =
          if (phrases.nonEmpty)
            phrases.map(_._2).sum / phrases.size
          else
            1.0

        grammarList.addFromString(
          grammar,
          grammarWeight(weight)
        )

        underlying.grammars =
          grammarList

        BiasMethod.Grammar

      case None =>
        BiasMethod.Unsupported
    }
  }

  private def createGrammar(
                             phrases: Seq[String]
                           ): String = {

    val alternatives =
      phrases
        .map(escapeJsgf)
        .mkString(" | ")

    s"""
       |#JSGF V1.0;
       |grammar contextual_bias;
       |public <phrase> = $alternatives;
       |""".stripMargin
  }

  private def escapeJsgf(
                          value: String
                        ): String = {

    val escaped =
      value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    s""""$escaped""""
  }

  private def grammarWeight(
                             boost: Double
                           ): Double = {
    // SpeechGrammar weight and SpeechRecognitionPhrase
    // boost don't use the same scale.
    //
    // Keep grammar weight in the traditional 0..1 range.
    (boost / 10.0)
      .max(0.0)
      .min(1.0)
  }
}


// ---------------------------------------------------------------------------
// High-level TTS wrapper
// ---------------------------------------------------------------------------

final class TextToSpeech private[speech] () {

  def voices: Seq[SpeechSynthesisVoice] =
    SpeechSynthesis.getVoices().toSeq

  def speaking: Boolean =
    SpeechSynthesis.speaking

  def pending: Boolean =
    SpeechSynthesis.pending

  def paused: Boolean =
    SpeechSynthesis.paused

  def speak(
             text: String,
             lang: String = "",
             rate: Double = 1.0,
             pitch: Double = 1.0,
             volume: Double = 1.0,
             voice: Option[SpeechSynthesisVoice] = None
           ): Future[Boolean] = {

    val utterance =
      new SpeechSynthesisUtterance(text)

    if (lang.nonEmpty)
      utterance.lang = lang

    utterance.rate = rate
    utterance.pitch = pitch
    utterance.volume = volume


    voice.foreach { v =>
      utterance.voice = v
    }

    BrowserConsole.log(utterance)

    SpeechSynthesis.speak(utterance)

    val promise = Promise[Boolean]

    utterance.onend = (_:js.Any) => { promise.success(true) }

    promise.future
  }

  def cancel(): Unit =
    SpeechSynthesis.cancel()

  def pause(): Unit =
    SpeechSynthesis.pause()

  def resume(): Unit =
    SpeechSynthesis.resume()

  def findVoice(
                 language: String
               ): Option[SpeechSynthesisVoice] =
    voices.find(
      _.lang.equalsIgnoreCase(language)
    )
}


@js.native
@JSGlobal("SpeechGrammarList")
class SpeechGrammarList() extends js.Object {

  val length: Int = js.native

  def addFromString(
                     string: String,
                     weight: Double = 1.0
                   ): Unit = js.native

  def addFromURI(
                  src: String,
                  weight: Double = 1.0
                ): Unit = js.native

  @JSBracketAccess
  def apply(index: Int): SpeechGrammar = js.native
}

@js.native
trait SpeechGrammar extends js.Object {

  var src: String = js.native

  var weight: Double = js.native
}


@js.native
@JSGlobal("SpeechRecognitionPhrase")
class SpeechRecognitionPhrase(
                               val phrase: String,
                               val boost: Double = 1.0
                             ) extends js.Object {


}