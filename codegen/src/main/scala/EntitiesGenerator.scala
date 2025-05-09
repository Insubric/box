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
       |  import ch.wsl.box.rest.utils.GeoJsonSupport._
       |
       |  import slick.model.ForeignKeyAction
       |  import slick.collection.heterogeneous._
       |  import slick.collection.heterogeneous.syntax._
       |  import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
       |  import org.locationtech.jts.geom.Geometry
       |
       |  import ch.wsl.box.model.UpdateTable
       |  import scala.concurrent.ExecutionContext
       |
       |object $container {
       |
       |      implicit val customConfig: Configuration = Configuration.default.withDefaults
       |      implicit def dec:Decoder[Array[Byte]] = Light.fileFormat
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
case class EntitiesGenerator(connection:Connection,model:Model,box_schema:String) extends slick.codegen.SourceCodeGenerator(model) with BoxSourceCodeGenerator with MyOutputHelper with Logging {




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
           |val encode$name:EncoderWithBytea[$name] = { e =>
           |  implicit def byteE = e
           |  Encoder.forProduct${columns.size}(${model.columns.map(_.name).mkString("\"","\",\"","\"")})(x =>
           |    ${columns.map(_.name).mkString("(x.",", x.",")")}
           |  )
           |}
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
    val encode$name:EncoderWithBytea[$name] = { e =>
      implicit def byteE = e
      deriveConfiguredEncoder[$name]
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

        val tableNameFull = model.name.schema.map(s => "\""+s+"\".").getOrElse("") + "\"" + model.name.table + "\""
        val tableName = model.name.schema.map(s => s+".").getOrElse("") + model.name.table

        val columnsLight = model.columns.map{c =>
          if(c.tpe == "Array[Byte]") s"""  substring("${c.name}" from 1 for 4096) as "${c.name}" """ else
            "\"" + c.name + "\""
        }.mkString(",")

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

  def boxGetResult = GR(r => $elementType($getResult))

  def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[$elementType]] = {
      val kv = keyValueComposer(this)
      val chunks = fields.flatMap(kv)
      if(chunks.nonEmpty) {
        val head = concat(sql\"\"\"update $tableNameFull set \"\"\",chunks.head)
        val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

        val returning = sql\"\"\" returning $columnsLight \"\"\"

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[$elementType](boxGetResult).head.map(x => Some(x))
      } else DBIO.successful(None)
    }

    override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[$elementType]] = {
      val sqlActionBuilder = concat(sql\"\"\"select $columnsLight from $tableNameFull \"\"\",where)
      sqlActionBuilder.as[$elementType](boxGetResult)
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
            new PgInformationSchema(box_schema:String,model.table.schema.getOrElse("public"),model.table.table).hasDefault(
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