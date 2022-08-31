package ch.wsl.box.jdbc


import scala.language.existentials
import PostgresProfile._
import PostgresProfile.api._
import slick.ast.{Bind, CompiledStatement, Node, OptionApply, ProductNode, Pure, ResultSetMapping, Select, TableExpansion, TypeMapping}
import slick.jdbc.{GetResult, JdbcResultConverterDomain, SQLActionBuilder, SetParameter}
import slick.relational.{CompiledMapping, ResultConverter}
import slick.util.{ConstArray, SQLBuilder}

import scala.collection.immutable


// https://github.com/shiraeeshi/slick-update-returning-example

object SlickUpdateExt {
  object UpdateReturning {

    // see https://stackoverflow.com/questions/30212785/slick-3-0-update-columns-in-a-table-and-return-whole-table-object/31545270#31545270

    implicit class UpdateReturningInvoker[E, U, C[_]](updateQuery: Query[E, U, C]) {

      private def extract(children:ConstArray[Node]) = children.map {
        case Select(_, col) => col.name
        case OptionApply(col) =>
          col match {
            case Select(_, childCol) => childCol.name
          }
      }.toSeq

      def updateReturning[A, F](returningQuery: Query[A, F, C], v: U) = {
        val ResultSetMapping(_,
        CompiledStatement(_, sres: SQLBuilder.Result, _),
        CompiledMapping(_updateConverter, _)) = updateCompiler.run(updateQuery.toNode).tree

        val returningNode = returningQuery.toNode
        val fieldNames: immutable.Seq[String] = returningNode match {
          case Bind(_, _, Pure(Select(_, col), _)) =>
            List(col.name)
          case Bind(_, _, Pure(ProductNode(children), _)) =>
            children.map { case Select(_, col) => col.name }.toSeq
          case Bind(_, _, TableExpansion(_, _, slick.ast.TypeMapping(ProductNode(children), _, _))) =>
            children.map { case Select(_, col) => col.name }.toSeq

          case Pure(Select(_, col), _) =>
            List(col.name)
          case Pure(ProductNode(children), _) =>
            children.map { case Select(_, col) => col.name }.toSeq
          case TableExpansion(_, _, slick.ast.TypeMapping(ProductNode(children), _, _)) =>
            extract(children)
          case TableExpansion(_, _, slick.ast.TypeMapping(slick.ast.TypeMapping(ProductNode(children), _, _), _, _)) =>
            extract(children)
        }

        implicit val pconv: SetParameter[U] = {
          val ResultSetMapping(_, _, CompiledMapping(converter: ResultConverter[JdbcResultConverterDomain, U], _)) =
            updateCompiler.run(updateQuery.toNode).tree
          SetParameter[U] { (value, params) =>
            converter.set(value, params.ps)
          }
        }

        implicit val rconv: GetResult[F] = {
          val ResultSetMapping(_, _, CompiledMapping(converter: ResultConverter[JdbcResultConverterDomain, F], _)) =
            queryCompiler.run(returningNode).tree
          GetResult[F] { p => converter.read(p.rs) }
        }

        val fieldsExp = fieldNames.map(quoteIdentifier).mkString(", ")
        val pconvUnit = pconv.applied(v)
        val sql = sres.sql + s" RETURNING $fieldsExp"

        SQLActionBuilder(List(sql), pconvUnit).as[F]
      }
    }

  }
}
