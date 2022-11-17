package ch.wsl.box.model


sealed trait DbOps

case object Update extends DbOps
case object Select extends DbOps

