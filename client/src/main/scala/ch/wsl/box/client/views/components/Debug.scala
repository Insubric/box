package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.UI
import io.udash.properties.model.ModelProperty
import scalatags.JsDom.all._
import io.udash._
import io.circe.syntax._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom.Event

object Debug {


  def apply[T](model: ReadableProperty[T],releaser:Binding=>Binding = b => b, name:String ="")(implicit enc:io.circe.Encoder[T]) = {
    val show = Property {
      false
    }

    val out = model.transform { m =>
      m.asJson.spaces2
    }
    if (UI.debug) {
      div(
        a(""),
        releaser(produce(show) {
            case true => div(
              a("Hide debug " + name, onclick :+= {(e: Event) => show.set(false); e.preventDefault() }),
              pre(bind(out))
            ).render
            case false => a("Show debug " + name, onclick :+= {(e: Event) => show.set(true); e.preventDefault()}).render
        }
      ))
    } else frag()
  }
}
