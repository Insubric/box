package ch.wsl.box.client.utils

object Shorten {
  def apply(str:String,maxLength:Int = 40):String = {
    val noHTML = ch.wsl.typings.striptags.mod.apply(str)
    if(noHTML.length > maxLength) {
      str.take(maxLength-3) + "..."
    } else str
  }
}
