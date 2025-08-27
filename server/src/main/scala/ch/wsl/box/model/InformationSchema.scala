package ch.wsl.box.model

import ch.wsl.box.information_schema.PgInformationSchema
import ch.wsl.box.rest.runtime.Registry
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

object InformationSchema {
  def table(t:String)(implicit e:ExecutionContext):PgInformationSchema = new PgInformationSchema(Registry.box().schema,Registry().schema,t)

  def roles():DBIO[Seq[String]] = {
    val boxSchema = Registry.box().schema
      sql"""select rolname from #$boxSchema.v_roles""".as[String]
  }

}
