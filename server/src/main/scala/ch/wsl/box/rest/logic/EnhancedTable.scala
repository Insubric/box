package ch.wsl.box.rest.logic

import akka.stream.Materializer
import scribe.Logging
import slick.lifted.{AbstractTable, ColumnOrdered, TableQuery}

import scala.reflect.runtime.universe._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{JSONSort, Sort}
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.{ColType, Registry, RegistryInstance}
import ch.wsl.box.rest.utils.UserProfile

import scala.concurrent.ExecutionContext


case class Col(rep:Rep[_],`type`:ColType)


/**
  * Created by andreaminetti on 16/02/16.
  *
  * to retrieve the instance of a column
  */

object EnhancedTable  extends Logging {

  implicit class EnTable[T](t: Table[_]) {

    val table = t

    private val rm = scala.reflect.runtime.currentMirror

    private def accessor(field: String): MethodSymbol = {
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


    private def rep(field: String) = rm.reflect(t).reflectMethod(accessor(field)).apply().asInstanceOf[ch.wsl.box.jdbc.PostgresProfile.api.Rep[_]]

    def col(field: String,registry:RegistryInstance)(implicit ec: ExecutionContext): Col = {
      Col(
        rep(field),
        typ(field,registry)
      )
    }

    def typ(field: String,registry:RegistryInstance) = {
      EntityMetadataFactory.fieldType(t.tableName, field,registry).getOrElse(ColType.unknown)
    }


  }



}
