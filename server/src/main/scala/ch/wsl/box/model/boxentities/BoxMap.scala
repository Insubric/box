package ch.wsl.box.model.boxentities

import ch.wsl.box.rest.runtime.Registry
import slick.model.ForeignKeyAction
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

object BoxMap {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  private val schema = Some(Registry.box().schema)

  import profile._


  case class Map_row(map_id: java.util.UUID, name: String, parameters: Option[List[String]] = None, srid: Int, x_min: Double, y_min: Double, x_max: Double, y_max: Double,max_zoom:Double)


  /** GetResult implicit for fetching Map_row objects using plain SQL queries */

  /** Table description of table map. Objects of this class serve as prototypes for rows in queries. */
  class Maps(_tableTag: Tag) extends Table[Map_row](_tableTag, schema, "maps") {


    def * = (map_id, name, parameters, srid, x_min, y_min, x_max, y_max, max_zoom).<>(Map_row.tupled, Map_row.unapply)

    /** Maps whole row to an option. Useful for outer joins. */

    /** Database column map_id SqlType(uuid), PrimaryKey */
    val map_id: Rep[java.util.UUID] = column[java.util.UUID]("map_id", O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column parameters SqlType(_text), Default(None) */
    val parameters: Rep[Option[List[String]]] = column[Option[List[String]]]("parameters", O.Default(None))
    val srid: Rep[Int] = column[Int]("srid")
    val x_min: Rep[Double] = column[Double]("x_min")
    val y_min: Rep[Double] = column[Double]("y_min")
    val x_max: Rep[Double] = column[Double]("x_max")
    val y_max: Rep[Double] = column[Double]("y_max")
    val max_zoom: Rep[Double] = column[Double]("max_zoom")
  }

  /** Collection-like TableQuery object for table Map */
  lazy val Maps = new TableQuery(tag => new Maps(tag))


  /** Entity class storing rows of table Map_layer_vector_db
   *
   * @param layer_id      Database column layer_id SqlType(uuid), PrimaryKey
   * @param map_id        Database column map_id SqlType(uuid)
   * @param entity        Database column entity SqlType(text)
   * @param field         Database column field SqlType(text)
   * @param geometry_type Database column geometry_type SqlType(text)
   * @param z_index       Database column z_index SqlType(int4), Default(None)
   * @param extra         Database column extra SqlType(jsonb), Default(None)
   * @param editable      Database column editable SqlType(bool), Default(false)
   * @param query         Database column query SqlType(jsonb), Default(None) */
  case class Map_layer_vector_db_row(layer_id: Option[java.util.UUID] = None, map_id: java.util.UUID, entity: String, field: String, geometry_type: String, z_index: Int, layer_order: Int, extra: Option[io.circe.Json] = None, editable: Boolean = false, query: Option[io.circe.Json] = None, srid:Int, autofocus:Boolean, color:String)


  /** Table description of table map_layer_vector_db. Objects of this class serve as prototypes for rows in queries. */
  class Map_layer_vector_db(_tableTag: Tag) extends Table[Map_layer_vector_db_row](_tableTag,schema, "map_layer_vector_db") {


    def * = (Rep.Some(layer_id), map_id, entity, field, geometry_type, z_index, layer_order, extra, editable, query, srid,autofocus,color).<>(Map_layer_vector_db_row.tupled, Map_layer_vector_db_row.unapply)


    /** Database column layer_id SqlType(uuid), PrimaryKey */
    val layer_id: Rep[java.util.UUID] = column[java.util.UUID]("layer_id", O.PrimaryKey, O.AutoInc)
    /** Database column map_id SqlType(uuid) */
    val map_id: Rep[java.util.UUID] = column[java.util.UUID]("map_id")
    /** Database column entity SqlType(text) */
    val entity: Rep[String] = column[String]("entity")
    /** Database column field SqlType(text) */
    val field: Rep[String] = column[String]("field")
    val color: Rep[String] = column[String]("color")
    /** Database column geometry_type SqlType(text) */
    val geometry_type: Rep[String] = column[String]("geometry_type")
    /** Database column z_index SqlType(int4), Default(None) */
    val z_index: Rep[Int] = column[Int]("z_index")
    val layer_order: Rep[Int] = column[Int]("layer_order")
    /** Database column extra SqlType(jsonb), Default(None) */
    val extra: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("extra", O.Default(None))
    /** Database column editable SqlType(bool), Default(false) */
    val editable: Rep[Boolean] = column[Boolean]("editable", O.Default(false))
    val autofocus: Rep[Boolean] = column[Boolean]("autofocus", O.Default(false))
    /** Database column query SqlType(jsonb), Default(None) */
    val query: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("query", O.Default(None))
    val srid: Rep[Int] = column[Int]("srid")

  }

  /** Collection-like TableQuery object for table Map_layer_vector_db */
  lazy val Map_layer_vector_db = new TableQuery(tag => new Map_layer_vector_db(tag))

  /** Entity class storing rows of table Map_layer_wmts
   *
   * @param layer_id         Database column layer_id SqlType(uuid), PrimaryKey
   * @param map_id           Database column map_id SqlType(uuid)
   * @param capabilities_url Database column capabilities_url SqlType(text)
   * @param wmts_layer_id    Database column wmts_layer_id SqlType(text)
   * @param srid             Database column srid SqlType(int4)
   * @param z_index          Database column z_index SqlType(int4), Default(None)
   * @param extra            Database column extra SqlType(jsonb), Default(None) */
  case class Map_layer_wmts_row(layer_id: Option[java.util.UUID] = None, map_id: java.util.UUID, capabilities_url: String, wmts_layer_id: String, srid: Int, z_index: Int, layer_order: Int, extra: Option[io.circe.Json] = None)



  /** GetResult implicit for fetching Map_layer_wmts_row objects using plain SQL queries */

  /** Table description of table map_layer_wmts. Objects of this class serve as prototypes for rows in queries. */
  class Map_layer_wmts(_tableTag: Tag) extends Table[Map_layer_wmts_row](_tableTag,schema, "map_layer_wmts") {

    def * = (Rep.Some(layer_id), map_id, capabilities_url, wmts_layer_id, srid, z_index, layer_order, extra).<>(Map_layer_wmts_row.tupled, Map_layer_wmts_row.unapply)

    /** Maps whole row to an option. Useful for outer joins. */

    /** Database column layer_id SqlType(uuid), PrimaryKey */
    val layer_id: Rep[java.util.UUID] = column[java.util.UUID]("layer_id", O.PrimaryKey, O.AutoInc)
    /** Database column map_id SqlType(uuid) */
    val map_id: Rep[java.util.UUID] = column[java.util.UUID]("map_id")
    /** Database column capabilities_url SqlType(text) */
    val capabilities_url: Rep[String] = column[String]("capabilities_url")
    /** Database column wmts_layer_id SqlType(text) */
    val wmts_layer_id: Rep[String] = column[String]("wmts_layer_id")
    /** Database column srid SqlType(int4) */
    val srid: Rep[Int] = column[Int]("srid")
    /** Database column z_index SqlType(int4), Default(None) */
    val z_index: Rep[Int] = column[Int]("z_index")
    val layer_order: Rep[Int] = column[Int]("layer_order")
    /** Database column extra SqlType(jsonb), Default(None) */
    val extra: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("extra", O.Default(None))

  }

  lazy val Map_layer_wmts = new TableQuery(tag => new Map_layer_wmts(tag))


  case class Map_layer_i18n_row(layer_id: java.util.UUID, lang: String, label: String)




  /** GetResult implicit for fetching Map_layer_i18n_row objects using plain SQL queries */

  /** Table description of table map_layer_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Map_layer_i18n(_tableTag: Tag) extends Table[Map_layer_i18n_row](_tableTag, schema, "map_layer_i18n") {


    def * = (layer_id, lang, label).<>(Map_layer_i18n_row.tupled, Map_layer_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */

    /** Database column layer_id SqlType(uuid) */
    val layer_id: Rep[java.util.UUID] = column[java.util.UUID]("layer_id")
    /** Database column lang SqlType(text) */
    val lang: Rep[String] = column[String]("lang")
    /** Database column label SqlType(text) */
    val label: Rep[String] = column[String]("label")


  }
  /** Collection-like TableQuery object for table Map_layer_i18n */
  lazy val Map_layer_i18n = new TableQuery(tag => new Map_layer_i18n(tag))


}
