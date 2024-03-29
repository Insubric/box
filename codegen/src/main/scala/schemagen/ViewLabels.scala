package schemagen

import ch.wsl.box.jdbc.Connection
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import ch.wsl.box.jdbc.PostgresProfile.api._
import scribe.Logging

import scala.concurrent.ExecutionContext



class ViewLabels(langs:Seq[String],boxSchema:String) extends Logging {

  private val keys = langs.map(l => s"$l.label as $l").mkString(", ")
  private val joins = langs.map(l => s"left join $boxSchema.labels $l on keys.key = $l.key and $l.lang='$l'").mkString("\n")
  private val update = langs.map(l => s"UPDATE $boxSchema.labels SET label = NEW.$l WHERE key = NEW.key and lang='$l';").mkString("\n")

  private val updateFunction =
    s"""
       |
       |""".stripMargin

  private val insert = langs.map(l => s"('$l',NEW.key,NEW.$l)").mkString("",",",";")

  def addVLabel(connection:Connection)(implicit ec:ExecutionContext) = connection.dbConnection.run {
    val q =
      sqlu"""
       drop trigger if exists v_labels_update on #$boxSchema.v_labels;
       drop trigger if exists v_labels_insert on #$boxSchema.v_labels;
       drop view if exists #$boxSchema.v_labels;

       create view #$boxSchema.v_labels AS
       with keys as (
           select distinct key
           from #$boxSchema.labels
       )
       select
              keys.key as key,
              #$keys
       from keys
       #$joins
       ;

       CREATE OR REPLACE FUNCTION #$boxSchema.v_labels_update() RETURNS trigger LANGUAGE plpgsql AS $$$$
       BEGIN
           #$update
           RETURN NEW;
       END $$$$;

       CREATE OR REPLACE FUNCTION #$boxSchema.v_labels_insert() RETURNS trigger LANGUAGE plpgsql AS $$$$
       BEGIN
           INSERT INTO #$boxSchema.labels (lang, key, label) values #$insert
           RETURN NEW;
       END $$$$;

       CREATE TRIGGER v_labels_update
           INSTEAD OF UPDATE ON #$boxSchema.v_labels
           FOR EACH ROW EXECUTE PROCEDURE #$boxSchema.v_labels_update();

       CREATE TRIGGER v_labels_insert
           INSTEAD OF INSERT ON #$boxSchema.v_labels
           FOR EACH ROW EXECUTE PROCEDURE #$boxSchema.v_labels_insert();

        grant select,update,insert on #$boxSchema.v_labels to box_translator;

       """
//    q.statements.map(x => println(x))
    q.transactionally
  }.map{ i =>
    logger.info(s"Added v_labels view $i")
  }.recover{ case t =>
    t.printStackTrace()
    logger.error(t.getMessage)
  }

  def run(connection:Connection)(implicit ec:ExecutionContext) = {
    for{
      r1 <- addVLabel(connection)
    } yield true
  }

}
