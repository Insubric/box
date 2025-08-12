package ch.wsl.box.services.file

import scala.concurrent.{ExecutionContext, Future}
import ch.wsl.box.model.shared.JSONID

/**
 *
 * @param rowId JSON id of the row
 * @param column formatted in the form <tablename>.<columnname>
 */
case class FileId(rowId:JSONID,column:String) {
  def name(mime:Option[String],tpe:String):String = {
    val extension = mime match {
      case Some(s) if s.endsWith("jpeg") => ".jpg"
      case Some(s) if s.endsWith("png") => ".png"
      case _ => ""
    }

    asString(tpe) + extension

  }

  def asString(tpe:String):String = s"$column-${rowId.asString}-$tpe"
}

sealed trait FileCacheKey{
  def id:FileId

  protected def prefix = id.asString(this.getClass.getSimpleName.toLowerCase)
  def asString(): String

}
case class Thumbnail(id:FileId,width:Int,height:Int) extends FileCacheKey {
  def asString(): String = s"$prefix-w$width-h$height"
}
case class Width(id:FileId,width:Int) extends FileCacheKey {
  def asString(): String = s"$prefix-w$width"
}
case class Cover(id:FileId,width:Int,height:Int) extends FileCacheKey {
  def asString(): String = s"$prefix-w$width-h$height"
}
case class Fit(id:FileId,width:Int,height:Int,color:String) extends FileCacheKey {
  def asString(): String = s"$prefix-w$width-h$height-c$color"
}

trait ImageCacheStorage {
  def clearField(id:FileId)(implicit ex:ExecutionContext):Future[Boolean]
  def save(fileId: FileCacheKey,data:Array[Byte])(implicit ex:ExecutionContext):Future[Boolean]
  def delete(fileId: FileCacheKey)(implicit ex:ExecutionContext):Future[Boolean]
  def get(fileId: FileCacheKey)(implicit ex:ExecutionContext):Future[Option[Array[Byte]]]
}