package ch.wsl.box.jdbc

case class PublicSchema(name:String)

object PublicSchema{
  def default = PublicSchema("public")
}
