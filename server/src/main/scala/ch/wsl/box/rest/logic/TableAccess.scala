package ch.wsl.box.rest.logic

import ch.wsl.box.db.SQLComposer
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, FullDatabase, UserDatabase}
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

object TableAccess extends Logging {




  def apply(table:String,schema:String,user:String,db:UserDatabase)(implicit ec:ExecutionContext) = db.run {
    sql"""select HAS_TABLE_PRIVILEGE(rolname, concat($schema, '."', $table,'"'), 'insert') as insert,
                 HAS_TABLE_PRIVILEGE(rolname, concat($schema, '."', $table,'"'), 'update') as update,
                 HAS_TABLE_PRIVILEGE(rolname, concat($schema, '."', $table,'"'), 'delete') as delete
          from pg_roles where rolname=$user
       """.as[(Boolean, Boolean, Boolean)].headOption
  }.map{
    case Some((i,u,d)) => ch.wsl.box.model.shared.TableAccess(i,u,d)
    case _ => {
      logger.warn("Can't read privileges from Information schema, defaulting to false;")
      ch.wsl.box.model.shared.TableAccess(false,false,false)
    }
  }


  def rowAccess(table:String, schema:String, user:String, query:JSONQuery, db:FullDatabase)(implicit ec:ExecutionContext):Future[Boolean] = {

      val composer = new SQLComposer(Some(schema),table,Registry())
      import composer._

      for{
        with_check <- db.adminDb.run(sql"""
         SELECT with_check FROM pg_policies where schemaname=$schema and tablename=$table and roles && (select array[rolname] || memberof from box.v_roles where rolname=$user)
         """.as[String])
        result <- if(with_check.isEmpty)
          Future.successful(Some(true))
        else
          db.db.run(concat(sql" select count(*) > 0 from #${fullyQualifiedName} ",concat(whereBuilder(query.copy(sort = List(),paging = None)),sql""" and #${with_check.head} limit 1""")).as[Boolean].headOption)
      } yield result.getOrElse(false)


  }








}
