package ch.wsl.box.codegen

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.jdbc.PostgresProfile.api._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.flywaydb.core.api.output.MigrateResult

import scala.concurrent.{Await, ExecutionContext, Future}

object MigrateDB {



  def box(connection:Connection,schema:String)(implicit ex:ExecutionContext) = {

    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .sqlMigrationPrefix("BOX_V")
      //.undoSqlMigrationPrefix("BOX_U") oly for pro or enterprise version
      .repeatableSqlMigrationPrefix("BOX_R")
      .schemas(schema)
      .defaultSchema(schema)
      .table("flyway_schema_history_box")
      .locations("box_migrations", "classpath:box_migrations")
      .ignoreMigrationPatterns("*:missing")
      .dataSource(connection.dataSource("BOX Migration", schema))
      .load()


    def migrationExceptions() = {
      if(flyway.info().current() != null)
        connection.dbConnection.run {
          sqlu"""
          update #$schema.flyway_schema_history_box set checksum=-766518484 where version='2';
          update #$schema.flyway_schema_history_box set checksum=1952033218 where version='3';
          update #$schema.flyway_schema_history_box set checksum=-1038081266 where version='4';
          update #$schema.flyway_schema_history_box set checksum=-433221282 where version='5';
          update #$schema.flyway_schema_history_box set checksum=867977953 where version='6';
          update #$schema.flyway_schema_history_box set checksum=-675803754 where version='07';
          update #$schema.flyway_schema_history_box set checksum=640792895 where version='08';
          update #$schema.flyway_schema_history_box set checksum=-2083187704 where version='09';
          update #$schema.flyway_schema_history_box set checksum=-2027628572 where version='10';
          update #$schema.flyway_schema_history_box set checksum=-741718079 where version='11';
          update #$schema.flyway_schema_history_box set checksum=-802282516 where version='12';
          update #$schema.flyway_schema_history_box set checksum=-115616180 where version='13';
          update #$schema.flyway_schema_history_box set checksum=-1493223802 where version='14';
          update #$schema.flyway_schema_history_box set checksum=1546038969 where version='15';
          update #$schema.flyway_schema_history_box set checksum=295109050 where version='16';
          update #$schema.flyway_schema_history_box set checksum=125955543 where version='18';
          update #$schema.flyway_schema_history_box set checksum=-559032529 where version='19';
          update #$schema.flyway_schema_history_box set checksum=-673805002 where version='20';
          update #$schema.flyway_schema_history_box set checksum=-471705743 where version='21';
          update #$schema.flyway_schema_history_box set checksum=2135196365 where version='22';
          update #$schema.flyway_schema_history_box set checksum=2004359677 where version='23';
          update #$schema.flyway_schema_history_box set checksum=-1284365639 where version='24';
          update #$schema.flyway_schema_history_box set checksum=1886963030 where version='25';
          update #$schema.flyway_schema_history_box set checksum=-1780826851 where version='26';
          update #$schema.flyway_schema_history_box set checksum=-1766308645 where version='27';
          update #$schema.flyway_schema_history_box set checksum=-35810 where version='28';
          update #$schema.flyway_schema_history_box set checksum=87342898 where version='29';
          update #$schema.flyway_schema_history_box set checksum=1973816606 where version='30';
          update #$schema.flyway_schema_history_box set checksum=-1547648294 where version='31';
          update #$schema.flyway_schema_history_box set checksum=-1466682231 where version='32';
          update #$schema.flyway_schema_history_box set checksum=-1174361640 where version='33';
          update #$schema.flyway_schema_history_box set checksum=1850442938 where version='35';
          update #$schema.flyway_schema_history_box set checksum=1818024207 where version='36';
          update #$schema.flyway_schema_history_box set checksum=-1468940163 where version='37';
          update #$schema.flyway_schema_history_box set checksum=-1162390581 where version='38';
          update #$schema.flyway_schema_history_box set checksum=760066478 where version='39';
          update #$schema.flyway_schema_history_box set checksum=829529451 where version='40';
          update #$schema.flyway_schema_history_box set checksum=597865581 where version='41';
          update #$schema.flyway_schema_history_box set checksum=-1556843576 where version='42';
          update #$schema.flyway_schema_history_box set checksum=427677514 where version='43';
          update #$schema.flyway_schema_history_box set checksum=-1993354810 where version='44';
          update #$schema.flyway_schema_history_box set checksum=-1116796032 where version='45';
          update #$schema.flyway_schema_history_box set checksum=-1585503788 where version='46';
          update #$schema.flyway_schema_history_box set checksum=-599563475 where version='47';
          update #$schema.flyway_schema_history_box set checksum=960086764 where version='48';
          update #$schema.flyway_schema_history_box set checksum=-765022474 where version='51';

          INSERT INTO #$schema.flyway_schema_history_box (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
          select (select count(*)+1 from #$schema.flyway_schema_history_box), '34', 'roles function permission', 'SQL', 'BOX_V34__roles_function_permission.sql', 938281884, 'postgres', now(), 4, true from (
              select * from (values ('34')) as t  where (select true from #$schema.flyway_schema_history_box where version = '2') except (select version from #$schema.flyway_schema_history_box where version ='34')
          ) as t;

          """.transactionally
        }
      else Future.successful(true)
    }

    {for{
      _ <- migrationExceptions()
      result <- Future { flyway.migrate() }
    } yield result}.recover{ case t:Throwable => t.printStackTrace()}

  }

  def app(connection:Connection)(implicit ex:ExecutionContext) = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .schemas(connection.dbSchema)
      .defaultSchema(connection.dbSchema)
      .table("flyway_schema_history")
      .locations("migrations","classpath:migrations")
      .ignoreMigrationPatterns("*:missing","*:future")
      .dataSource(connection.dataSource("App migration",connection.dbSchema))
      .load()

    val result = flyway.migrate()
    result
  }



}