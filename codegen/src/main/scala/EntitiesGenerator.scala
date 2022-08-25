package ch.wsl.box.codegen

import ch.wsl.box.information_schema.PgInformationSchema
import ch.wsl.box.jdbc.{Connection, Managed, TypeMapping}
import com.typesafe.config.Config
import slick.model.Model
import slick.ast.ColumnOption
import scribe.Logging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt


trait MyOutputHelper extends slick.codegen.OutputHelpers {
  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]) : String= {
    s"""
       |package ${pkg}
       |// AUTO-GENERATED Slick data model
       |/** Stand-alone Slick data model for immediate use */
       |
       |
       |  import io.circe._
       |  import io.circe.generic.extras.semiauto._
       |  import io.circe.generic.extras.Configuration
       |  import ch.wsl.box.rest.utils.JSONSupport._
       |  import Light._
       |
       |  import slick.model.ForeignKeyAction
       |  import slick.collection.heterogeneous._
       |  import slick.collection.heterogeneous.syntax._
       |  import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
       |  import org.locationtech.jts.geom.Geometry
       |
       |  import ch.wsl.box.model.UpdateTable
       |
       |object $container {
       |
       |      implicit val customConfig: Configuration = Configuration.default.withDefaults
       |
       |      import ch.wsl.box.jdbc.PostgresProfile.api._
       |
       |      val profile = ch.wsl.box.jdbc.PostgresProfile
       |
       |          ${indent(code)}
       |}
     """.stripMargin.trim
  }
}

//exteded code generator (add route and registry generation)
case class EntitiesGenerator(connection:Connection,model:Model) extends slick.codegen.SourceCodeGenerator(model) with BoxSourceCodeGenerator with MyOutputHelper with Logging {




  // override table generator
  override def Table = new Table(_){

    //disable plain override mapping, with hlist produces compile time problem with non-primitives types
    override def PlainSqlMapper = new PlainSqlMapperDef {
      override def code = ""
    }

    // disable entity class generation and mapping
    override def EntityType = new EntityType{


      def encoderDecoder:String =
        s"""
           |val decode$name:Decoder[$name] = Decoder.forProduct${columns.size}(${model.columns.map(_.name).mkString("\"","\",\"","\"")})($name.apply)
           |val encode$name:Encoder[$name] = Encoder.forProduct${columns.size}(${model.columns.map(_.name).mkString("\"","\",\"","\"")})(x =>
           |  ${columns.map(_.name).mkString("(x.",", x.",")")}
           |)
           |""".stripMargin

      override def code = {
        val args = columns.map { c =>
          c.default.map(v =>
            s"${c.name}: ${c.exposedType} = $v"
          ).getOrElse(
            s"${c.name}: ${c.exposedType}"
          )
        }.mkString(", ")

        val prns = (parents.take(1).map(" extends "+_) ++ parents.drop(1).map(" with "+_)).mkString("")
        val result = s"""case class $name($args)$prns"""

        if(model.columns.size <= 22) {
          result +
            s"""
               |
               |$encoderDecoder
               |
               |""".stripMargin
        } else {
          result + s"""

    val decode$name:Decoder[$name] = deriveConfiguredDecoder[$name]
    val encode$name:Encoder[$name] = deriveConfiguredEncoder[$name]

    object ${TableClass.elementType}{

      type ${TableClass.elementType}HList = ${columns.map(_.exposedType).mkString(" :: ")} :: HNil

      def factoryHList(hlist:${TableClass.elementType}HList):${TableClass.elementType} = {
        val x = hlist.toList
        ${TableClass.elementType}("""+columns.zipWithIndex.map(x => "x("+x._2+").asInstanceOf["+x._1.exposedType+"]").mkString(",")+s""");
      }

      def toHList(e:${TableClass.elementType}):Option[${TableClass.elementType}HList] = {
        Option(( """+columns.map(c => "e."+ c.name + " :: ").mkString("")+s""" HNil))
      }
    }
                 """
        }

      }
    }



    override def factory = if(model.columns.size <= 22 ) {
      super.factory
    } else {

      s"${TableClass.elementType}.factoryHList"

    }

    override def extractor = if(model.columns.size <= 22 ) {
      super.extractor
    } else {
      s"${TableClass.elementType}.toHList"
    }

    override def mappingEnabled = true;

    override def TableClass = new TableClassDef {


      override def optionEnabled = columns.size <= 22 && mappingEnabled && columns.exists(c => !c.model.nullable)

      override def code: String =  {
        val prns = parents.map(" with " + _).mkString("")
        val args = model.name.schema.map(n => s"""Some("$n")""") ++ Seq("\""+model.name.table+"\"")

        val getResult = columns.map{c =>
          c.exposedType match {
            case "Option[java.util.UUID]" => "r.nextUUIDOption"
            case "java.util.UUID" => "r.nextUUID"
            case typ:String if typ.contains("Option[List[") => typ.replace("Option[List[","r.nextArrayOption[").dropRight(1) + ".map(_.toList)"
            case typ:String if typ.contains("List[") => typ.replace("List[","r.nextArray[") + ".toList"
            case _ => "r.<<"
          }
        }.mkString(",")

        s"""
class $name(_tableTag: Tag) extends Table[$elementType](_tableTag, ${args.mkString(", ")})$prns with UpdateTable[$elementType] {

  def updateReturning(fields:Map[String,Json],where:Map[String,Json]):DBIO[$elementType] = {
      val kv = keyValueComposer(this)
      val head = concat(sql\"\"\"update ${model.name.schema.map(s => s+".").getOrElse("") + model.name.table} set \"\"\",kv(fields.head))
      val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }
      val whereBuilder = where.tail.foldLeft(concat(sql" where ",kv(where.head))){ case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

      val returning = sql\"\"\" returning ${model.columns.map(_.name).mkString(",")}\"\"\"

      val sqlActionBuilder = concat(concat(set,whereBuilder),returning)
      sqlActionBuilder.as[$elementType](GR(r => $elementType($getResult))).head
    }

  ${indent(body.map(_.mkString("\n")).mkString("\n\n"))}
}
        """.trim()
      }
    }

    def tableModel = model

    override def ForeignKey = new ForeignKeyDef(_) {
      //override def code: String = "" // dont generate foreign keys
    }

    override def Column = new Column(_){

      private val hasDefault:Boolean = {
//        val dbDefault = Await.result(
//          connection.dbConnection.run(
//            PgInformationSchema.hasDefault(
//              model.table.schema.getOrElse("public"),
//              model.table.table,
//              model.name
//            )
//          ),
//          10.seconds
//        ) // this breaks #162 due to a Slick limitation
        val explicitDefault = Managed.hasTriggerDefault(model.table.table,model.name)
        explicitDefault  // || dbDefault
      }

      private val primaryWithDefault:Boolean = {
        val dbDefault = Await.result(
          connection.dbConnection.run(
            PgInformationSchema.hasDefault(
              model.table.schema.getOrElse("public"),
              model.table.table,
              model.name
            )
          ),
          10.seconds
        ) // this breaks #162 due to a Slick limitation
        val explicitDefault = Managed.hasTriggerDefault(model.table.table,model.name)
        (explicitDefault  || dbDefault) && primaryKey
      }




      // customize Scala column names
      override def rawName = model.name



      override def rawType: String =  TypeMapping(model).getOrElse(super.rawType)


      private def primaryKey = {
        val singleKey = model.options.contains(ColumnOption.PrimaryKey)
        val multipleKey = tableModel.primaryKey.exists(_.columns.exists(_.name == model.name))
        singleKey || multipleKey
      }


      private val managed:Boolean = Managed(model.table.table) && primaryKey

      override def asOption: Boolean =  (managed || hasDefault || primaryWithDefault) && !model.nullable match { //add no model nullable condition to avoid double optionals
        case true => true
        case false => super.asOption
      }


      override def options: Iterable[String] = {
        val opts = { (managed || primaryWithDefault) match {
          case false => super.options
          case true => {super.options.toSeq ++ Seq("O.AutoInc")}.distinct
        }}.filterNot{ opt => hasDefault && opt.startsWith("O.Default") }
        opts
      }


    }


  }

}