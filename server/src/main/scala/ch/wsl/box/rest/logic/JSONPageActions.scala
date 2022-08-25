package ch.wsl.box.rest.logic

import ch.wsl.box
import ch.wsl.box.jdbc
import ch.wsl.box.jdbc.PostgresProfile
import ch.wsl.box.model.shared.{IDs, JSONCount, JSONDiff, JSONID, JSONKeyValue, JSONQuery}
import io.circe.Json
import slick.dbio.DBIO

object JSONPageActions extends TableActions[Json] {
  private val responseId = JSONID(Vector(JSONKeyValue("static",Json.fromString("page"))))

  override def insert(obj: Json): PostgresProfile.api.DBIO[Json] = DBIO.successful(Json.obj())

  override def delete(id: JSONID): PostgresProfile.api.DBIO[Int] = DBIO.successful(0)

  override def update(id: JSONID, obj: Json): PostgresProfile.api.DBIO[Json] = DBIO.successful(Json.obj())

  override def updateDiff(diff: JSONDiff):DBIO[Seq[JSONID]] = DBIO.successful(Seq(responseId))

  override def find(query: JSONQuery) = DBIO.successful(Seq())

  override def getById(id: JSONID): PostgresProfile.api.DBIO[Option[Json]] = DBIO.successful(Some(Json.obj()))

  override def count(): PostgresProfile.api.DBIO[JSONCount] = DBIO.successful(JSONCount(0))

  override def count(query: JSONQuery): PostgresProfile.api.DBIO[Int] = DBIO.successful(0)

  override def ids(query: JSONQuery): PostgresProfile.api.DBIO[IDs] = DBIO.successful(IDs(true,0,Seq(),0))

  override def updateField(id: JSONID, fieldName: String, value: Json):DBIO[Json] = DBIO.successful(Json.obj())
}
