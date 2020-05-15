package ch.wsl.box.rest.logic

import scribe.Logging
import slick.lifted.{AbstractTable, TableQuery}

import scala.reflect.runtime.universe._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.ColType

import scala.concurrent.ExecutionContext


case class Col(rep:Rep[_],`type`:ColType)


/**
 * Created by andreaminetti on 16/02/16.
  *
  * to retrieve the instance of a column
 */


object EnhancedTable extends Logging {

  implicit class EnTable[T](t: Table[_]) {


    private val rm = scala.reflect.runtime.currentMirror

    private def accessor(field:String):MethodSymbol = {
      try {

          rm.classSymbol(t.getClass).toType.members.collectFirst {
            case m: MethodSymbol if m.name.toString == field => m
          }.get
        } catch {
          case e: Exception => {
            logger.debug(rm.classSymbol(t.getClass).toType.members.toString)
            throw new Exception(s"Field not found:$field available fields: ${rm.classSymbol(t.getClass).toType.members} of table:${t.tableName}")
          }
        }
      }


    def col(field: String)(implicit ec:ExecutionContext):Col = Col(
      rm.reflect(t).reflectMethod(accessor(field)).apply().asInstanceOf[ch.wsl.box.jdbc.PostgresProfile.api.Rep[_]],
      typ(field)
    )

    def typ(field:String)(implicit ec:ExecutionContext) = {
      EntityMetadataFactory.fieldType(t.tableName,field)
    }


  }

}

