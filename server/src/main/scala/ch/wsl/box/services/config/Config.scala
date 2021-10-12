package ch.wsl.box.services.config

trait Config {
  def boxSchemaName:Option[String]
  def schemaName:String
}
