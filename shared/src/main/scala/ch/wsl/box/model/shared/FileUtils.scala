package ch.wsl.box.model.shared

object FileUtils {

  val base = "keepOriginal;"
  private val baseBase64 = "a2VlcE9yaWdpbmFsOw=="
  def keep(mime:String) = base + mime
  def isKeep(str:String) = str.startsWith(base) || str.startsWith(baseBase64)

  def extractMime(keep:String):String = keep.stripPrefix(base)

}
