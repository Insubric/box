package ch.wsl.box.model.shared

object FileUtils {

  private val base = "keepOriginal;"
  def keep(mime:String) = base + mime
  def isKeep(str:String) = str.startsWith(base)

  def extractMime(keep:String):String = keep.stripPrefix(base)

}
