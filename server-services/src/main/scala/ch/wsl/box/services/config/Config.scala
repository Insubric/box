package ch.wsl.box.services.config

trait Config {
  def boxSchemaName: String

  def schemaName: String

  def postgisSchemaName:String

  def langs: Seq[String]

  def frontendUrl:String
  def basePath:String = frontendUrl.split("/").drop(3).toList match {
    case Nil => "/"
    case x => x.mkString("/","/","/")
  }
}
