package ch.wsl.box.rest.logic

import ch.wsl
import ch.wsl.box
import ch.wsl.box.jdbc
import ch.wsl.box.jdbc.PostgresProfile
import ch.wsl.box.model.shared.{IDs, JSONCount, JSONDiff, JSONID, JSONKeyValue, JSONLookups, JSONLookupsRequest, JSONQuery, JSONQueryFilter}
import io.circe.Json
import slick.dbio.DBIO

object JSONPageActions extends TableActions[Json] {
  private val responseId = JSONID(Vector(JSONKeyValue("static",Json.fromString("page"))))

  override def insert(obj: Json): PostgresProfile.api.DBIO[Json] = DBIO.successful(Json.obj())

  override def delete(id: JSONID): PostgresProfile.api.DBIO[Int] = DBIO.successful(0)

  override def update(id: JSONID, obj: Json): PostgresProfile.api.DBIO[Json] = DBIO.successful(Json.obj())

  override def updateDiff(diff: JSONDiff):DBIO[Seq[JSONID]] = DBIO.successful(Seq(responseId))

  override def findSimple(q:JSONQuery): wsl.box.jdbc.PostgresProfile.api.DBIO[Seq[Json]] = DBIO.successful(Seq())


  override def fetchFields(fields: Seq[String], query: JSONQuery): _root_.ch.wsl.box.jdbc.PostgresProfile.api.DBIO[Seq[Json]] = DBIO.successful(Seq())

  override def distinctOn(field: String, query: JSONQuery): _root_.ch.wsl.box.jdbc.PostgresProfile.api.DBIO[Seq[Json]] = DBIO.successful(Seq())

  override def getById(id: JSONID): PostgresProfile.api.DBIO[Option[Json]] = DBIO.successful(Some(Json.obj()))

  override def count(): PostgresProfile.api.DBIO[JSONCount] = DBIO.successful(JSONCount(0))

  override def count(query: JSONQuery): PostgresProfile.api.DBIO[Int] = DBIO.successful(0)

  override def ids(query: JSONQuery,keys:Seq[String]): PostgresProfile.api.DBIO[IDs] = DBIO.successful(IDs(true,0,Seq(),0))

  override def updateField(id: JSONID, fieldName: String, value: Json):DBIO[Json] = DBIO.successful(Json.obj())

  override def lookups(request: JSONLookupsRequest): _root_.ch.wsl.box.jdbc.PostgresProfile.api.DBIO[Seq[JSONLookups]] = DBIO.successful(Seq())
}
