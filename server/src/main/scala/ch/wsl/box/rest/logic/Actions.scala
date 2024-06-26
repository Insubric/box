package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.model.shared.{IDs, JSONCount, JSONDiff, JSONID, JSONLookups, JSONLookupsRequest, JSONMetadata, JSONQuery, JSONQueryFilter}
import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import akka.stream.scaladsl.Source
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.GeoJson.Geometry
import io.circe.Json
import slick.dbio.{DBIOAction, Effect, Streaming}
import slick.lifted.MappedProjection



trait ViewActions[T] {

  def findSimple(query:JSONQuery): DBIO[Seq[T]]

  def fetchFields(fields:Seq[String],query:JSONQuery):DBIO[Seq[Json]]

  def fetchGeom(properties:Seq[String],field:String,query:JSONQuery):DBIO[Seq[(Json,Geometry)]]

  def distinctOn(fields: Seq[String],query:JSONQuery): DBIO[Seq[Json]]

  def getById(id: JSONID=JSONID.empty):DBIO[Option[T]]

  def count(): DBIO[JSONCount]
  def count(query: JSONQuery): DBIO[Int]

  def ids(query: JSONQuery,keys:Seq[String]): DBIO[IDs]

  def lookups(request:JSONLookupsRequest):DBIO[Seq[JSONLookups]]
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
  def insert(obj: T): DBIO[T]

  def delete(id:JSONID): DBIO[Int]

  def update(id:JSONID, obj: T): DBIO[T]

  def updateDiff(diff:JSONDiff):DBIO[Option[T]]

}
