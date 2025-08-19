package ch.wsl.box.rest.logic

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, UserDatabase}
import scribe.Logging

import scala.concurrent.ExecutionContext

object TableAccess extends Logging {

//  def queryRoles(table:String,schema:String,user:String) =
//                      """select a.tablename,b.usename,
//                      |  HAS_TABLE_PRIVILEGE(usename, concat(schemaname, '.', tablename), 'select') as select,
//                      |  HAS_TABLE_PRIVILEGE(usename, concat(schemaname, '.', tablename), 'insert') as insert,
//                      |  HAS_TABLE_PRIVILEGE(usename, concat(schemaname, '.', tablename), 'update') as update,
//                      |  HAS_TABLE_PRIVILEGE(usename, concat(schemaname, '.', tablename), 'delete') as delete,
//                      |  HAS_TABLE_PRIVILEGE(usename, concat(schemaname, '.', tablename), 'references') as references  from pg_tables a , pg_user b
//                      |where a.schemaname=$schema and a.tablename=$table and usename=$user;"""
//



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

//  def write(table:String,schema:String,user:String)(implicit ec:ExecutionContext) = Auth.adminDB.run {
//    sql"""SELECT 1
//          FROM information_schema.role_table_grants
//          WHERE table_name=$table and table_schema=$schema and grantee=$user and privilege_type='UPDATE'""".as[Int].headOption.map(_.isDefined)
//  }










}
