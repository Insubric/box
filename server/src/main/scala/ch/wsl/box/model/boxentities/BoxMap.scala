package ch.wsl.box.model.boxentities

import ch.wsl.box.rest.runtime.Registry
import slick.model.ForeignKeyAction
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

object BoxMap {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  private val schema = Some(Registry.box().schema)

  import profile._

  /** Entity class storing rows of table Map
   *
   * @param map_id     Database column map_id SqlType(uuid), PrimaryKey
   * @param name       Database column name SqlType(text)
   * @param parameters Database column parameters SqlType(_text), Default(None)
   * @param srid       Database column srid SqlType(int4), Default(None)
   * @param x_min      Database column x_min SqlType(float8), Default(None)
   * @param y_min      Database column y_min SqlType(float8), Default(None)
   * @param x_max      Database column x_max SqlType(float8), Default(None)
   * @param y_max      Database column y_max SqlType(float8), Default(None) */
  case class Map_row(map_id: java.util.UUID, name: String, parameters: Option[List[String]] = None, srid: Option[Int] = None, x_min: Option[Double] = None, y_min: Option[Double] = None, x_max: Option[Double] = None, y_max: Option[Double] = None)


  /** GetResult implicit for fetching Map_row objects using plain SQL queries */

  /** Table description of table map. Objects of this class serve as prototypes for rows in queries. */
  class Maps(_tableTag: Tag) extends Table[Map_row](_tableTag, schema, "maps") {


    def * = (map_id, name, parameters, srid, x_min, y_min, x_max, y_max).<>(Map_row.tupled, Map_row.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(map_id), Rep.Some(name), parameters, srid, x_min, y_min, x_max, y_max)).shaped.<>({ r => import r._; _1.map(_ => Map_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column map_id SqlType(uuid), PrimaryKey */
    val map_id: Rep[java.util.UUID] = column[java.util.UUID]("map_id", O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column parameters SqlType(_text), Default(None) */
    val parameters: Rep[Option[List[String]]] = column[Option[List[String]]]("parameters", O.Default(None))
    /** Database column srid SqlType(int4), Default(None) */
    val srid: Rep[Option[Int]] = column[Option[Int]]("srid", O.Default(None))
    /** Database column x_min SqlType(float8), Default(None) */
    val x_min: Rep[Option[Double]] = column[Option[Double]]("x_min", O.Default(None))
    /** Database column y_min SqlType(float8), Default(None) */
    val y_min: Rep[Option[Double]] = column[Option[Double]]("y_min", O.Default(None))
    /** Database column x_max SqlType(float8), Default(None) */
    val x_max: Rep[Option[Double]] = column[Option[Double]]("x_max", O.Default(None))
    /** Database column y_max SqlType(float8), Default(None) */
    val y_max: Rep[Option[Double]] = column[Option[Double]]("y_max", O.Default(None))
  }

  /** Collection-like TableQuery object for table Map */
  lazy val Maps = new TableQuery(tag => new Maps(tag))

  /** Entity class storing rows of table Map_layers
   *
   * @param layer_id      Database column layer_id SqlType(uuid), PrimaryKey
   * @param map_id        Database column map_id SqlType(uuid)
   * @param name          Database column name SqlType(text), Default(None)
   * @param geometry_type Database column geometry_type SqlType(text)
   * @param z_index       Database column z_index SqlType(int4), Default(None)
   * @param extra         Database column extra SqlType(jsonb), Default(None)
   * @param editable      Database column editable SqlType(bool), Default(false)
   * @param entity        Database column entity SqlType(text), Default(None)
   * @param query         Database column query SqlType(jsonb), Default(None) */
  case class Map_layers_row(layer_id: java.util.UUID, map_id: java.util.UUID, name: String, geometry_type: String, z_index: Option[Int] = None, extra: Option[io.circe.Json] = None, editable: Boolean = false, entity: Option[String] = None, query: Option[io.circe.Json] = None)



  /** GetResult implicit for fetching Map_layers_row objects using plain SQL queries */

  /** Table description of table map_layers. Objects of this class serve as prototypes for rows in queries. */
  class Map_layers(_tableTag: Tag) extends Table[Map_layers_row](_tableTag, schema, "map_layers")  {



    def * = (layer_id, map_id, name, geometry_type, z_index, extra, editable, entity, query).<>(Map_layers_row.tupled, Map_layers_row.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(layer_id), Rep.Some(map_id), name, geometry_type, z_index, extra, Rep.Some(editable), entity, query)).shaped.<>({ r => import r._; _1.map(_ => Map_layers_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7.get, _8, _9))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column layer_id SqlType(uuid), PrimaryKey */
    val layer_id: Rep[java.util.UUID] = column[java.util.UUID]("layer_id", O.PrimaryKey)
    /** Database column map_id SqlType(uuid) */
    val map_id: Rep[java.util.UUID] = column[java.util.UUID]("map_id")
    /** Database column name SqlType(text), */
    val name: Rep[String] = column[String]("name")
    /** Database column geometry_type SqlType(text), Default(None) */
    val geometry_type: Rep[String] = column[String]("geometry_type")
    /** Database column z_index SqlType(int4), Default(None) */
    val z_index: Rep[Option[Int]] = column[Option[Int]]("z_index", O.Default(None))
    /** Database column extra SqlType(jsonb), Default(None) */
    val extra: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("extra", O.Default(None))
    /** Database column editable SqlType(bool), Default(false) */
    val editable: Rep[Boolean] = column[Boolean]("editable", O.Default(false))
    /** Database column entity SqlType(text), Default(None) */
    val entity: Rep[Option[String]] = column[Option[String]]("entity", O.Default(None))
    /** Database column query SqlType(jsonb), Default(None) */
    val query: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("query", O.Default(None))

    /** Foreign key referencing Map (database name map_layers_map_id_fkey) */
    lazy val mapFk = foreignKey("map_layers_map_id_fkey", map_id, Maps)(r => r.map_id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table Map_layers */
  lazy val Map_layers = new TableQuery(tag => new Map_layers(tag))


}
