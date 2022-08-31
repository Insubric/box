package ch.wsl.box.services.config

trait Config {
  def boxSchemaName: Option[String]

  def schemaName: String

  def langs: Seq[String]

  def frontendUrl:String
  def basePath:String = frontendUrl.split("/").drop(3).toList match {
    case Nil => "/"
    case x => x.mkString("/","/","/")
  }
}
