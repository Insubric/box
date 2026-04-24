package ch.wsl.box.client.views.components.widget.`trait`

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.views.components.widget.Widget
import io.circe.{Decoder, Json}
import io.udash.ReadableProperty

trait HasParams { this:Widget =>

  def allData:ReadableProperty[Json]

  def params[T](key:String)(implicit dec:Decoder[T]):ReadableProperty[Option[T]] = allData.transform{ d =>
    for{
      js <- field.getParam(key,d)
      v <- js.as[T].toOption
    } yield v
  }

}
