package ch.wsl.box.client.mocks

import ch.wsl.box.client.services.REST
import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.{BoxDefinition}
import ch.wsl.box.model.shared.{Child, ExportDef, FormActionsMetadata, IDs, JSONCount, JSONField, JSONFieldMap, JSONFieldTypes, JSONID, JSONKeyValue, JSONLookup, JSONMetadata, JSONQuery, Layout, LayoutBlock, LoginRequest, NewsEntry, SharedLabels, TableAccess, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import io.circe.syntax._
import org.scalajs.dom.File
import scribe.Logging

import scala.concurrent.Future

class RestMock(values:Values) extends REST with Logging {
  override def version(): Future[String] = Future.successful("version")

  override def appVersion(): Future[String] = Future.successful("appVersion")

  override def validSession(): Future[Boolean] = Future.successful{
    true
  }

  override def cacheReset(): Future[String] = {
    println("cacheReset not implemented")
    ???
  }

  override def serverReset(): Future[String] = {
    println("serverReset not implemented")
    ???
  }

  override def entities(kind: String): Future[Seq[String]] = {
    kind match {
      case "form" => Future.successful(values.formEntities)
      case _ => {
        println(s"entities for $kind not implemented")
        ???
      }
    }
  }

  override def specificKind(kind: String, lang: String, entity: String): Future[String] = {
    println("specificKind not implemented")
    ???
  }

  override def list(kind: String, lang: String, entity: String, limit: Int): Future[Seq[Json]] = {
    println("list1 not implemented")
    ???
  }

  override def list(kind: String, lang: String, entity: String, query: JSONQuery): Future[Seq[Json]] = {
    println("list2 not implemented")
    ???
  }

  override def csv(kind: String, lang: String, entity: String, q: JSONQuery): Future[Seq[Seq[String]]] = {
    println("csv not implemented")
    ???
  }

  override def count(kind: String, lang: String, entity: String): Future[Int] = {
    println("count not implemented")
    ???
  }

  override def keys(kind: String, lang: String, entity: String): Future[Seq[String]] = {
    println("keys not implemented")
    ???
  }

  override def ids(kind: String, lang: String, entity: String, q: JSONQuery): Future[IDs] = {
    println("ids not implemented")
    ???
  }

  override def metadata(kind: String, lang: String, entity: String, public:Boolean): Future[JSONMetadata] = Future.successful{
    values.metadata
  }

  override def tabularMetadata(kind: String, lang: String, entity: String): Future[JSONMetadata] = {
    println("tabularMetadata not implemented")
    ???
  }

  override def children(kind: String, entity: String, lang: String, public:Boolean): Future[Seq[JSONMetadata]] = Future.successful{
    Seq(values.childMetadata,values.subchildMetadata)
  }

  override def lookup(lang: String, lookupEntity: String, map: JSONFieldMap, queryWithSubstitutions: Json): Future[Seq[JSONLookup]] = {
    println("lookup not implemented")
    ???
  }

  override def get(kind: String, lang: String, entity: String, id: JSONID): Future[Json] = Future.successful{
    values.get(id)
  }

  override def update(kind: String, lang: String, entity: String, id: JSONID, data: Json): Future[JSONID] = {
    Future.successful(values.update(id,data))
  }

  override def insert(kind: String, lang: String, entity: String, data: Json, public:Boolean): Future[JSONID] = Future.successful{
    JSONID(id = Vector(JSONKeyValue("id","1")))
  }

  override def delete(kind: String, lang: String, entity: String, id: JSONID): Future[JSONCount] = {
    println("delete not implemented")
    ???
  }

  override def sendFile(file: File, id: JSONID, entity: String): Future[Int] = {
    println("sendFile not implemented")
    ???
  }

  override def login(request: LoginRequest): Future[Json] = Future.successful{
    Json.True
  }

  override def logout(): Future[String] = {
    println("logout not implemented")
    ???
  }

  override def labels(lang: String): Future[Map[String, String]] = {
    Future.successful(lang match {
      case "en" => Map(
        SharedLabels.header.lang -> values.headerLangEn
      )
      case "it" => Map(
        SharedLabels.header.lang -> values.headerLangIt
      )
    })
  }

  override def conf(): Future[Map[String, String]] = Future.successful{
    values.conf
  }

  override def ui(): Future[Map[String, String]] = Future.successful{
    values.uiConf
  }

  override def news(lang: String): Future[Seq[NewsEntry]] = {
    println("news not implemented")
    ???
  }

  override def dataMetadata(kind: String, name: String, lang: String): Future[JSONMetadata] = {
    println("dataMetadata not implemented")
    ???
  }

  override def dataDef(kind: String, name: String, lang: String): Future[ExportDef] = {
    println("dataDef not implemented")
    ???
  }

  override def dataList(kind: String, lang: String): Future[Seq[ExportDef]] = {
    println("dataList not implemented")
    ???
  }

  override def data(kind: String, name: String, params: Json, lang: String): Future[Seq[Seq[String]]] = {
    println("data not implemented")
    ???
  }

  override def tableAccess(table: String, kind: String): Future[TableAccess] = {
    println("table Access not implemented")
    ???
  }

  override def generateStub(entity: String): Future[Boolean] = {
    println("generateStub not implemented")
    ???
  }

  override def definition(): Future[BoxDefinition] = ???

  override def definitionDiff(definition: BoxDefinition): Future[BoxDefinitionMerge] = ???

  override def definitionCommit(merge: BoxDefinitionMerge): Future[Boolean] = ???
}
