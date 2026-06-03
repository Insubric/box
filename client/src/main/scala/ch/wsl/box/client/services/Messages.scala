package ch.wsl.box.client.services

import ch.wsl.box.client.viewmodel.Row
import ch.wsl.box.model.shared.JSONID
import io.circe.Json
import io.udash.Registration
import io.udash.properties.single.Property

sealed trait Message

trait Messages {
  def pub(m:Message):Unit
  def sub(action:Message => Any):Registration
}

object Messages {
  case object Empty extends Message
  case class RowHover(row:Row) extends Message
  case class RowOut(row:Row) extends Message
}


class MessagesPropertyImpl extends Messages {

  val prop:Property[Message] = Property(Messages.Empty)

  override def pub(m: Message): Unit = prop.set(m)

  override def sub(action: Message => Any): Registration = prop.listen(action)
}