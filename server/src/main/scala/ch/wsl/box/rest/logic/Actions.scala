package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.model.shared.{IDs, JSONCount, JSONID, JSONMetadata, JSONQuery}
import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import akka.stream.scaladsl.Source
import ch.wsl.box.jdbc.FullDatabase
import io.circe.Json
import slick.dbio.{DBIOAction, Effect, Streaming}
import slick.lifted.MappedProjection



trait ViewActions[T] {

  def find(query: JSONQuery=JSONQuery.empty): DBIO[Seq[T]] //enable streaming

  def getById(id: JSONID=JSONID.empty):DBIO[Option[T]]

  def count(): DBIO[JSONCount]
  def count(query: JSONQuery): DBIO[Int]

  def ids(query: JSONQuery): DBIO[IDs]
}

/**
 *
 * Modification actions always return the number of modificated rows
 * we decided to not return the model itself because in some cirumstances
 * it may exposes more information than required, in this way we assure
 * that data retrival system is uniform
 *
 * @tparam T model class type
 */
trait TableActions[T] extends ViewActions[T] {
  def insert(obj: T): DBIO[JSONID]
  def insertReturningModel(obj: T): DBIO[T]

  def delete(id:JSONID): DBIO[Int]

  def update(id:JSONID, obj: T): DBIO[Int]

  def updateIfNeeded(id:JSONID, obj: T): DBIO[Int]

  def upsertIfNeeded(id:Option[JSONID], obj: T): DBIO[JSONID]

  def updateField(id:JSONID,fieldName:String,value:Json)

}

trait JsonQuery{
  def findQuery(query: JSONQuery): Query[MappedProjection[Json, _], Json, Seq]
}
