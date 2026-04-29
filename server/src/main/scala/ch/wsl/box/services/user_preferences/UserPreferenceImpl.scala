package ch.wsl.box.services.user_preferences

import cats.effect.unsafe.implicits.global
import ch.wsl.box.jdbc.Connection
import io.circe.Json
import skunk._
import skunk.circe.codec.all._
import cats.effect._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop

import scala.concurrent.Future

class UserPreferenceImpl(connection:Connection) extends UserPreference {

  private val selectQ:Query[String,Json] = sql"""
            select preferences from #${connection.boxSchema}.user_preferences where username = $text
            """.query(jsonb)

  private val setQ:Command[String *: Json *: EmptyTuple] = sql"""
            insert into #${connection.boxSchema}.user_preferences (username,preferences) values ($text,$jsonb)
            on conflict (username) do update set preferences=excluded.preferences;
            """.command

  override def get(username: String): Future[Option[Json]] = {
    connection.pooledAdminSession().use{ p => p.use{ s =>
      for {
        d <- s.option(selectQ)(username)
      } yield d
    }}
  }.unsafeToFuture()

  override def set(username: String, data: Json): Future[Boolean] = {
    connection.pooledAdminSession().use{ p => p.use{ s =>
      for {
        d <- s.execute(setQ)(username,data)
      } yield true
    }}
  }.unsafeToFuture()
}
