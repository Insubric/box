package ch.wsl.box.rest.logic

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, UserDatabase}

import scala.concurrent.ExecutionContext

object TableAccess {

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
    sql"""select HAS_TABLE_PRIVILEGE(usename, concat($schema, '."', $table,'"'), 'insert') as insert,
                 HAS_TABLE_PRIVILEGE(usename, concat($schema, '."', $table,'"'), 'update') as update,
                 HAS_TABLE_PRIVILEGE(usename, concat($schema, '."', $table,'"'), 'delete') as delete
          from pg_user where usename=$user
       """.as[(Boolean, Boolean, Boolean)].headOption
  }.map(_.getOrElse((false, false, false))).map(x=> ch.wsl.box.model.shared.TableAccess(x._1,x._2,x._3))

//  def write(table:String,schema:String,user:String)(implicit ec:ExecutionContext) = Auth.adminDB.run {
//    sql"""SELECT 1
//          FROM information_schema.role_table_grants
//          WHERE table_name=$table and table_schema=$schema and grantee=$user and privilege_type='UPDATE'""".as[Int].headOption.map(_.isDefined)
//  }










}
