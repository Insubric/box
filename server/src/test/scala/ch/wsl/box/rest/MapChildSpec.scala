package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared.GeoJson.CRS
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, MapMetadata, MapProjection, POLYGON, WMTS}
import ch.wsl.box.model.shared.{CurrentUser, EntityKind, Filter, GeoJson, JSONField, JSONFieldTypes, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter, WidgetsNames}
import _root_.io.circe.Json
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.rest.metadata.MetadataFactory
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, GeoJsonSupport, UserProfile}
import ch.wsl.box.services.Services
import ch.wsl.box.testmodel.Entities.{App_child, App_parent, Geo, Geo_row}
import org.scalatest.Assertion
import slick.dbio
import slick.dbio.DBIO

import java.util.UUID
import scala.concurrent.ExecutionContext
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.io.geotools.GeoJsonConverter
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import _root_.io.circe.syntax._
import GeoJson._

class MapChildSpec extends BaseSpec {

  val parentId = UUID.randomUUID()
  val idParent = "id"
  val parentName = "mapParent"
  val layerName = "Test DB"
  val map_name = "test_map"

  val proj = MapProjection("EPSG:21781","+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=600000 +y_0=200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs ")

  val vectorLayerId = UUID.fromString("81e1fb9d-866c-419e-90b3-beb9391078e2")
  val vectorLayer = DbVector(id = vectorLayerId, name = layerName, entity = "geo", field = "geo", entityPrimaryKey = Seq("id"), srid = proj, geometryType = POLYGON, query = Some(JSONQuery.filterWith(JSONQueryFilter("id", Some(Filter.EQUALS),fieldValue = Some("id"), value = None))), extra = Json.Null, editable = true, zIndex = 2, order = 3, autofocus = true, color = "rgb(204,0,0)")


  val metadata: JSONMetadata = JSONMetadata.simple(parentId,EntityKind.FORM.kind,"geo","it",Seq(
    JSONField.number(idParent,nullable = false),
    JSONField.string("label",nullable = false),
    JSONField(JSONFieldTypes.MAP,map_name,true,widget = Some(WidgetsNames.mapChild),map = Some(MapMetadata(
      id = UUID.randomUUID(),
      name = "test",
      parameters = Seq("id"),
      srid = proj,
      boundingBox = Box2d(xMin = 485071.58, yMin = 74261.72, xMax = 837119.8, yMax = 299941.79),
      maxZoom = 12,
      wmts = Seq(
        WMTS(id = UUID.randomUUID(),name = "SwissTopo Farbe", capabilitiesUrl = "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml", layerId = "ch.swisstopo.pixelkarte-farbe", srid = proj, order = 1, zIndex = 0,extra = Json.Null),
        WMTS(id = UUID.randomUUID(),name = "SwissTopo Grau", capabilitiesUrl = "https://wmts.geo.admin.ch/EPSG/21781/1.0.0/WMTSCapabilities.xml", layerId = "ch.swisstopo.pixelkarte-grau", srid = proj, order = 2, zIndex = 0,extra = Json.Null),
      ),
      db = Seq(
        vectorLayer
      ))
    ))
  ),Seq(idParent))



  val polygon:GeoJson.Geometry = GeoJson.Polygon(List(List(
    GeoJson.Coordinates(701298.315901753, 112883.97642683581),
    GeoJson.Coordinates(701311.2263166115, 112887.84955129334),
    GeoJson.Coordinates(701324.1367314699, 112891.72267575086),
    GeoJson.Coordinates(701332.7436747089, 112895.5958002084),
    GeoJson.Coordinates(701336.1864520045, 112907.64552074292),
    GeoJson.Coordinates(701334.0347161947, 112914.10072817214),
    GeoJson.Coordinates(701324.9974257938, 112922.27732424914),
    GeoJson.Coordinates(701315.0994410691, 112923.99871289692),
    GeoJson.Coordinates(701305.0917996713, 112943.13267918663),
    GeoJson.Coordinates(701294.4427772956, 112922.27732424914),
    GeoJson.Coordinates(701266.4568579326, 112937.25565243619),
    GeoJson.Coordinates(701275.5075021698, 112917.97385262966),
    GeoJson.Coordinates(701269.0522947407, 112910.65795087656),
    GeoJson.Coordinates(701273.786113522, 112893.87441156061),
    GeoJson.Coordinates(701271.2040305504, 112890.86198142698),
    GeoJson.Coordinates(701298.315901753, 112883.97642683581)
  )),crs = CRS("EPSG:21781"))


  val testMetadataFactory = new MetadataFactory{
    override def of(name: String, lang: String, user: CurrentUser)(implicit ec: ExecutionContext, services: Services): dbio.DBIO[JSONMetadata] = DBIO.successful{ metadata }

    override def of(id: UUID, lang: String, user: CurrentUser)(implicit ec: ExecutionContext, services: Services): dbio.DBIO[JSONMetadata] = DBIO.successful{ metadata }

    override def children(form: JSONMetadata, user: CurrentUser, ignoreChilds: Seq[UUID])(implicit ec: ExecutionContext, services: Services): dbio.DBIO[Seq[JSONMetadata]] = DBIO.successful(Seq())

    override def list(implicit ec: ExecutionContext, services: Services): dbio.DBIO[Seq[String]] = ???
  }


  val jsonIdInsert = { // when inserting a map on a db generated PK the value of the key in not known beforehand
    s"""
       |{
       |  "key" : "id",
       |}
       |""".stripMargin
  }

  def jsonIdUpdate(id:Int) =
    s"""
       |{
       |  "key" : "id",
       |  "value": $id
       |}
       |""".stripMargin




  "Map child"  should "delete geometries when empty on update"  in withServices[Assertion] { implicit services =>
    implicit val session = BoxSession(CurrentUser.simple(services.connection.adminUser))
    implicit val up = UserProfile.simple(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)


    def json(id:Int) = _root_.io.circe.parser.parse(
      s"""
         |{
         | "id": $id,
         | "$map_name": {
         |    "features" : [
         |      {
         |        "geometry" : {
         |          "type" : "Empty",
         |          "crs" : {
         |            "type" : "name",
         |            "properties" : {
         |              "name" : "EPSG:0"
         |            }
         |          }
         |        },
         |        "properties" : {
         |          "id" : {
         |            "id" : [
         |              {
         |                "key" : "id",
         |                "value": $id
         |              }
         |            ]
         |          },
         |          "field" : "geo",
         |          "entity" : "geo"
         |        },
         |        "type" : "Feature"
         |      }
         |    ]
         |  }
         |
         |}
         |
         |""".stripMargin).toOption.get

    val actions = FormActions(metadata,Registry(),testMetadataFactory)

    for{
      _ <- up.db.run(Geo.insertOrUpdate(Geo_row(Some(1),None,GeoJsonConverter.toJTS(polygon))))
      i <- up.db.run(Geo.result)
      id = i.head.id.get
      i <- up.db.run(actions.update(JSONID.fromMap(Map(idParent -> Json.fromInt(id)).toSeq),json(id)).transactionally)
      afterUpdate <- up.db.run(Geo.result)
    } yield {
      afterUpdate.length shouldBe 1
      afterUpdate.head.geo.isEmpty shouldBe true
    }

  }

  it should "delete geometries when empty on insert"  in withServices[Assertion] { implicit services =>
    implicit val session = BoxSession(CurrentUser.simple(services.connection.adminUser))
    implicit val up = UserProfile.simple(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    def json = _root_.io.circe.parser.parse(
      s"""
         |{
         | "$map_name": {
         |    "features" : [
         |      {
         |        "geometry" : {
         |          "type" : "Empty",
         |          "crs" : {
         |            "type" : "name",
         |            "properties" : {
         |              "name" : "EPSG:0"
         |            }
         |          }
         |        },
         |        "properties" : {
         |          "id" : {
         |            "id" : [
         |              {
         |                "key" : "id"
         |              }
         |            ]
         |          },
         |          "field" : "geo",
         |          "entity" : "geo"
         |        },
         |        "type" : "Feature"
         |      }
         |    ]
         |  }
         |
         |}
         |
         |""".stripMargin).toOption.get

    val actions = FormActions(metadata,Registry(),testMetadataFactory)

    for{
      i <- up.db.run(actions.insert(json).transactionally)
      afterUpdate <- up.db.run(Geo.result)
    } yield {
      afterUpdate.length shouldBe 1
      afterUpdate.head.geo.isEmpty shouldBe true
    }

  }



  it should "insert geometries on insert"  in withServices[Assertion] { implicit services =>
    implicit val session = BoxSession(CurrentUser.simple(services.connection.adminUser))
    implicit val up = UserProfile.simple(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    def json = _root_.io.circe.parser.parse(
      s"""
         |{
         | "$map_name": {
         |    "features" : [
         |      {
         |        "geometry" : ${polygon.asJson},
         |        "properties" : {
         |          "id" : {
         |            "id" : [
         |              {
         |                "key" : "id"
         |              }
         |            ]
         |          },
         |          "field" : "geo",
         |          "entity" : "geo"
         |        },
         |        "type" : "Feature"
         |      }
         |    ]
         |  }
         |
         |}
         |
         |""".stripMargin).toOption.get

    val actions = FormActions(metadata,Registry(),testMetadataFactory)

    for{
      i <- up.db.run(actions.insert(json).transactionally)
      afterUpdate <- up.db.run(Geo.result)
    } yield {
      afterUpdate.length shouldBe 1
      afterUpdate.head.geo.isDefined shouldBe true
      GeoJsonSupport.fromJTS(afterUpdate.head.geo.get) shouldBe polygon
    }

  }


}
