package ch.wsl.box.client.mocks

import ch.wsl.box.client.services.REST
import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.BoxDefinition
import ch.wsl.box.model.shared.GeoTypes.GeoData
import ch.wsl.box.model.shared.admin.FormCreationRequest
import ch.wsl.box.model.shared.geo.GeoDataRequest
import ch.wsl.box.model.shared.oidc.UserInfo
import ch.wsl.box.model.shared.{BoxTranslationsFields, CSVTable, Child, CurrentUser, ExportDef, Field, FormActionsMetadata, GeoJson, GeoTypes, IDs, JSONCount, JSONField, JSONFieldMap, JSONFieldTypes, JSONID, JSONKeyValue, JSONLookup, JSONLookups, JSONLookupsRequest, JSONMetadata, JSONQuery, Layout, LayoutBlock, LoginRequest, NewsEntry, PDFTable, SharedLabels, TableAccess, WidgetsNames, XLSTable}
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import io.circe.syntax._
import org.scalajs.dom.File
import scribe.Logging

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RestMock(values:Values) extends REST with Logging {
  override def version()(implicit ec:ExecutionContext): Future[String] = Future.successful("version")

  override def appVersion()(implicit ec:ExecutionContext): Future[String] = Future.successful("appVersion")

  override def validSession()(implicit ec:ExecutionContext): Future[Boolean] = Future.successful{
    true
  }


  override def me()(implicit ec:ExecutionContext): Future[UserInfo] = values.loggedUser match {
    case Some(value) => Future.successful(userInfo)
    case None => Future.failed(new Exception("Not logged in"))
  }

  override def authenticate(code: String, provider_id: String)(implicit ec: ExecutionContext): Future[UserInfo] = ???

  override def cacheReset()(implicit ec:ExecutionContext): Future[String] = {
    println("cacheReset not implemented")
    ???
  }

  override def entities(kind: String)(implicit ec:ExecutionContext): Future[Seq[String]] = {
    kind match {
      case "form" => Future.successful(values.formEntities)
      case _ => {
        println(s"entities for $kind not implemented")
        ???
      }
    }
  }

  override def specificKind(kind: String, lang: String, entity: String)(implicit ec:ExecutionContext): Future[String] = Future.successful {
    values.kind
  }

  override def list(kind: String, lang: String, entity: String, limit: Int)(implicit ec:ExecutionContext): Future[Seq[Json]] = {
    println("list1 not implemented")
    ???
  }

  override def list(kind: String, lang: String, entity: String, query: JSONQuery)(implicit ec:ExecutionContext): Future[Seq[Json]] = {
    println("list2 not implemented")
    ???
  }


  override def geoData(kind: String, lang: String, entity: String, field: String, request: GeoDataRequest,public:Boolean)(implicit ec: ExecutionContext): Future[GeoData] = Future.successful {
    values.geoData(entity,field)
  }

  override def csv(kind: String, lang: String, entity: String, q: JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[Seq[String]]] = Future.successful {
    values.csv(q)
  }

  override def count(kind: String, lang: String, entity: String)(implicit ec:ExecutionContext): Future[Int] = {
    println("count not implemented")
    ???
  }

  override def keys(kind: String, lang: String, entity: String)(implicit ec:ExecutionContext): Future[Seq[String]] = {
    println("keys not implemented")
    ???
  }

  override def ids(kind: String, lang: String, entity: String, q: JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[IDs] = Future.successful {
    values.ids
  }

  override def metadata(kind: String, lang: String, entity: String, public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata] = Future.successful{
    values.metadata
  }

  override def tabularMetadata(kind: String, lang: String, entity: String,public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata] = Future.successful{
    values.metadata
  }

  override def children(kind: String, entity: String, lang: String, public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONMetadata]] = Future.successful{
    values.children(entity)
  }

  override def lookup(kind:String, lang:String,entity:String,  field:String, query: JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONLookup]] = {
    println("lookup not implemented")
    ???
  }


  override def lookups(kind: String, lang: String, entity: String, fk: JSONLookupsRequest,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONLookups]] = Future.successful {
    values.lookups
  }

  override def get(kind: String, lang: String, entity: String, id: JSONID, public:Boolean)(implicit ec:ExecutionContext): Future[Json] = Future.successful{
    values.get(id)
  }


  override def maybeGet(kind: String, lang: String, entity: String, id: JSONID, public: Boolean)(implicit ec:ExecutionContext): Future[Option[Json]] = Future.successful {
    Some(values.get(id))
  }

  override def update(kind: String, lang: String, entity: String, id: JSONID, data: Json, public:Boolean)(implicit ec:ExecutionContext): Future[Json] = {
    Future.successful(values.update(id,data))
  }

  override def updateMany(kind: String, lang: String, entity: String, ids: Seq[JSONID], data: Seq[Json])(implicit ec:ExecutionContext): Future[Seq[Json]] = ???

  override def insert(kind: String, lang: String, entity: String, data: Json, public:Boolean)(implicit ec:ExecutionContext): Future[Json] = Future.successful{
    values.insert(data)
  }

  override def delete(kind: String, lang: String, entity: String, id: JSONID)(implicit ec:ExecutionContext): Future[JSONCount] = {
    println("delete not implemented")
    ???
  }


  override def deleteMany(kind: String, lang: String, entity: String, ids: Seq[JSONID])(implicit ec:ExecutionContext): Future[JSONCount] = ???

  override def sendFile(file: File, id: JSONID, entity: String)(implicit ec:ExecutionContext): Future[Int] = {
    println("sendFile not implemented")
    ???
  }

  val userInfo = UserInfo("t","t",None,Seq(),Json.Null)
  override def login(request: LoginRequest)(implicit ec:ExecutionContext): Future[UserInfo] = Future.successful{
    userInfo
  }

  override def logout()(implicit ec:ExecutionContext): Future[String] = {
    println("logout not implemented")
    ???
  }

  override def labels(lang: String)(implicit ec:ExecutionContext): Future[Map[String, String]] = {
    Future.successful(lang match {
      case "en" => Map(
        SharedLabels.header.lang -> values.headerLangEn
      )
      case "it" => Map(
        SharedLabels.header.lang -> values.headerLangIt
      )
    })
  }

  override def conf()(implicit ec:ExecutionContext): Future[Map[String, String]] = Future.successful{
    values.conf
  }

  override def ui()(implicit ec:ExecutionContext): Future[Map[String, String]] = Future.successful{
    values.uiConf
  }

  override def news(lang: String)(implicit ec:ExecutionContext): Future[Seq[NewsEntry]] = {
    println("news not implemented")
    ???
  }

  override def dataMetadata(kind: String, name: String, lang: String)(implicit ec:ExecutionContext): Future[JSONMetadata] = {
    println("dataMetadata not implemented")
    ???
  }

  override def dataDef(kind: String, name: String, lang: String)(implicit ec:ExecutionContext): Future[ExportDef] = {
    println("dataDef not implemented")
    ???
  }

  override def dataList(kind: String, lang: String)(implicit ec:ExecutionContext): Future[Seq[ExportDef]] = {
    println("dataList not implemented")
    ???
  }

  override def data(kind: String, name: String, params: Json, lang: String)(implicit ec:ExecutionContext): Future[Seq[Seq[String]]] = {
    println("data not implemented")
    ???
  }

  override def tableAccess(table: String, kind: String)(implicit ec:ExecutionContext): Future[TableAccess] = Future.successful {
    values.tableAccess
  }


  override def renderTable(table: PDFTable)(implicit ec:ExecutionContext): Future[String] = ???
  override def exportCSV(table: CSVTable)(implicit ec:ExecutionContext): Future[File] = ???
  override def exportXLS(table: XLSTable)(implicit ec:ExecutionContext): Future[File] = ???

  override def generateStub(entity: String)(implicit ec:ExecutionContext): Future[Boolean] = {
    println("generateStub not implemented")
    ???
  }

  override def childCandidates(table: String)(implicit ec: ExecutionContext): Future[Seq[String]] = ???


  override def roles()(implicit ec: ExecutionContext): Future[Seq[String]] = ???

  override def createForm(formRequest: FormCreationRequest)(implicit ec: ExecutionContext): Future[UUID] = ???

  override def definition()(implicit ec:ExecutionContext): Future[BoxDefinition] = ???

  override def definitionDiff(definition: BoxDefinition)(implicit ec:ExecutionContext): Future[BoxDefinitionMerge] = ???

  override def definitionCommit(merge: BoxDefinitionMerge)(implicit ec:ExecutionContext): Future[Boolean] = ???


  override def translationsFields(lang: String)(implicit ec:ExecutionContext): Future[Seq[Field]] = ???

  override def translationsFieldsCommit(merge: BoxTranslationsFields)(implicit ec:ExecutionContext): Future[Int] = ???

  override def execute(functionName: String, lang: String, data:Json)(implicit ec:ExecutionContext) = ???
}
