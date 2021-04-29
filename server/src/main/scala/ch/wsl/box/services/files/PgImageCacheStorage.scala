package ch.wsl.box.services.files
import ch.wsl.box.jdbc.Connection

import scala.concurrent.{ExecutionContext, Future}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxImageCache
import ch.wsl.box.model.shared.JSONID
import ch.wsl.box.services.file.{FileCacheKey, FileId, ImageCacheStorage}
import scribe.Logging
import wvlet.airframe.bind

class PgImageCacheStorage extends ImageCacheStorage with Logging {

  val connection = bind[Connection]

  override def save(fileId: FileCacheKey, data: Array[Byte])(implicit ex:ExecutionContext): Future[Boolean] = connection.adminDB.run{
    BoxImageCache.Table.insertOrUpdate(BoxImageCache.BoxImageCache_row(fileId.asString,data))
  }.map(_ => true)

  override def delete(fileId: FileCacheKey)(implicit  ex:ExecutionContext): Future[Boolean] = connection.adminDB.run{
    BoxImageCache.Table.filter(_.key === fileId.asString).delete
  }.map(_ => true)

  override def get(fileId: FileCacheKey)(implicit ex:ExecutionContext): Future[Option[Array[Byte]]] = connection.adminDB.run{
    BoxImageCache.Table.filter(_.key === fileId.asString).take(1).result
  }.map(_.headOption.map(_.data))

  override def clearField(id: FileId)(implicit ex: ExecutionContext): Future[Boolean] = connection.adminDB.run{
    val prefix = id.asString("")
    BoxImageCache.Table.filter(_.key.startsWith(prefix)).delete
  }.map{ rows =>
    logger.debug(s"Affected rows $rows")
    true
  }
}
