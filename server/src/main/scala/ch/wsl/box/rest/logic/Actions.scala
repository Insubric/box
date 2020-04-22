package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.model.shared.{IDs, JSONCount, JSONID, JSONQuery}
import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import akka.stream.scaladsl.Source

trait ViewActions[T] {
  def find(query: JSONQuery=JSONQuery.empty)(implicit db: Database, mat:Materializer): Future[Seq[T]] = Source.fromPublisher(findStreamed(query)).runFold(Seq[T]())(_ ++ Seq(_))

  def findStreamed(query: JSONQuery=JSONQuery.empty)(implicit db: Database): DatabasePublisher[T]

  def getById(id: JSONID=JSONID.empty)(implicit db: Database): Future[Option[T]]

  def count()(implicit db: Database): Future[JSONCount]

  def ids(query: JSONQuery)(implicit db: Database, mat:Materializer): Future[IDs]
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
  def insert(obj: T)(implicit db:Database):Future[JSONID]

  def delete(id:JSONID)(implicit db:Database):Future[Int]

  def update(id:JSONID, obj: T)(implicit db:Database):Future[Int]

  def updateIfNeeded(id:JSONID, obj: T)(implicit db:Database):Future[Int]

  def upsertIfNeeded(id:JSONID, obj: T)(implicit db:Database):Future[JSONID]
}