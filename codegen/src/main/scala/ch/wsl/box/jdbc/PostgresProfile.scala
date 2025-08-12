package ch.wsl.box.jdbc


import com.github.tminglei.slickpg._
import io.circe.Json
import slick.basic.Capability
import slick.dbio.DBIO
import slick.driver.JdbcProfile
import slick.jdbc.{PositionedParameters, PositionedResult, SetParameter}

import java.sql.JDBCType
import java.util.UUID


trait UUIDPlainImplicits {

  implicit class PgPositionedResult(val r: PositionedResult) {
    def nextUUID: UUID = UUID.fromString(r.nextString)

    def nextUUIDOption: Option[UUID] = r.nextStringOption().map(UUID.fromString)
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }

}

trait PostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  //  with PgRangeSupport
  //  with PgHStoreSupport
  //  with PgPlayJsonSupport
  with PgCirceJsonSupport
  with PgSearchSupport
  with PgPostGISSupport
  //  with PgNetSupport
  //  with PgLTreeSupport
{
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with CirceImplicits
    //    with NetImplicits
    //    with LTreeImplicits
    //    with RangeImplicits
    //    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with PostGISImplicits
    with PostGISAssistants
    with ByteaPlainImplicits
    with SimpleArrayPlainImplicits
    with Date2DateTimePlainImplicits
    with PostGISPlainImplicits
    with CirceJsonPlainImplicits
    //    with SimpleJsonPlainImplicits
    //    with SimpleNetPlainImplicits
    //    with SimpleLTreePlainImplicits
    //    with SimpleRangePlainImplicits
    //    with SimpleHStorePlainImplicits
    with SimpleSearchPlainImplicits
    with UUIDPlainImplicits
  {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val shortListTypeMapper = new SimpleArrayJdbcType[Short]("int2").to(_.toList)
    implicit val intListTypeMapper = new SimpleArrayJdbcType[Int]("int4").to(_.toList)
    implicit val longListTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toList)
    implicit val floatListTypeMapper = new SimpleArrayJdbcType[Float]("float4").to(_.toList)
    implicit val doubleListTypeMapper = new SimpleArrayJdbcType[Double]("float8").to(_.toList)
    implicit val bigdecimalListTypeMapper = new SimpleArrayJdbcType[java.math.BigDecimal]("numeric")
      .mapTo[scala.math.BigDecimal](javaBigDecimal => scala.math.BigDecimal(javaBigDecimal),
        scalaBigDecimal => scalaBigDecimal.bigDecimal).to(_.toList)
  }




//  val plainAPI = new API
//    with ByteaPlainImplicits
//    with SimpleArrayPlainImplicits
//    with Date2DateTimePlainImplicits
//    with PostGISPlainImplicits
//    with CirceJsonPlainImplicits
//    //    with SimpleJsonPlainImplicits
//    //    with SimpleNetPlainImplicits
//    //    with SimpleLTreePlainImplicits
//    //    with SimpleRangePlainImplicits
//    //    with SimpleHStorePlainImplicits
//    with SimpleSearchPlainImplicits {}



}

object PostgresProfile extends PostgresProfile
