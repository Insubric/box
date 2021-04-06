package ch.wsl.box.client.services.impl

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{HttpClient, REST}
import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.BoxDefinition
import ch.wsl.box.model.shared.{EntityKind, ExportDef, IDs, JSONCount, JSONFieldMap, JSONID, JSONLookup, JSONMetadata, JSONQuery, LoginRequest, NewsEntry, PDFTable, TableAccess}
import io.circe.{Decoder, Encoder, Json}
import kantan.csv.rfc
import kantan.csv._
import kantan.csv.ops._
import org.scalajs.dom.File
import scribe.Logging

import scala.concurrent.Future

class RestImpl(httpClient:HttpClient) extends REST with Logging {



  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.Formatters._
  import ch.wsl.box.client.Context._
  import ch.wsl.box.model.shared.EntityKind._

  def version() = httpClient.get[String](Routes.apiV1("/version"))
  def appVersion() = httpClient.get[String](Routes.apiV1("/app_version"))
  def validSession() = httpClient.get[Boolean](Routes.apiV1("/validSession"))
  def cacheReset() = httpClient.get[String](Routes.apiV1("/cache/reset"))
  def serverReset() = httpClient.get[String](Routes.apiV1("/server/reset"))

  def entities(kind:String):Future[Seq[String]] = httpClient.get[Seq[String]](Routes.apiV1(s"/${EntityKind(kind).plural}"))

  //for entities and forms
  def specificKind(kind:String, lang:String, entity:String):Future[String] = httpClient.get[String](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/kind") )    //distinguish entities into table or view
  def list(kind:String, lang:String, entity:String, limit:Int): Future[Seq[Json]] = httpClient.post[JSONQuery,Seq[Json]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/list"),JSONQuery.empty.limit(limit))
  def list(kind:String, lang:String, entity:String, query:JSONQuery): Future[Seq[Json]] = httpClient.post[JSONQuery,Seq[Json]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/list"),query)
  def csv(kind:String, lang:String, entity:String, q:JSONQuery): Future[Seq[Seq[String]]] = httpClient.post[JSONQuery,String](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/csv"),q).map{ result =>
    result.asUnsafeCsvReader[Seq[String]](rfc).toSeq
  }
  def count(kind:String, lang:String, entity:String): Future[Int] = httpClient.get[Int](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/count"))
  def keys(kind:String, lang:String, entity:String): Future[Seq[String]] = httpClient.get[Seq[String]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/keys"))
  def ids(kind:String, lang:String, entity:String, q:JSONQuery): Future[IDs] = httpClient.post[JSONQuery,IDs](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/ids"),q)
  def metadata(kind:String, lang:String, entity:String, public:Boolean): Future[JSONMetadata] = {
    val prefix = if(public) "/public" else ""
    httpClient.get[JSONMetadata](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/metadata"))
  }
  def tabularMetadata(kind:String, lang:String, entity:String): Future[JSONMetadata] = httpClient.get[JSONMetadata](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/tabularMetadata"))

  //only for forms
  def children(kind:String, entity:String, lang:String, public:Boolean): Future[Seq[JSONMetadata]] = {
    val prefix = if(public) "/public" else ""
    httpClient.get[Seq[JSONMetadata]](Routes.apiV1(s"$prefix/$kind/$lang/$entity/children"))
  }
  def lookup(lang:String,lookupEntity: String, map: JSONFieldMap, queryWithSubstitutions: Json): Future[Seq[JSONLookup]] = {
    queryWithSubstitutions.as[JSONQuery] match {
      case Right(query) => httpClient.post[JSONQuery, Seq[JSONLookup]](Routes.apiV1(s"/entity/$lang/$lookupEntity/lookup/${map.textProperty}/${map.valueProperty}"), query)
      case Left(fail) => {
        Future.successful(Seq())
      }
    }
  }


  //for entities and forms
  def get(kind:String, lang:String, entity:String, id:JSONID):Future[Json] = httpClient.get[Json](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${id.asString}"))
  def update(kind:String, lang:String, entity:String, id:JSONID, data:Json):Future[JSONID] = httpClient.put[Json,JSONID](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${id.asString}"),data)
  def updateMany(kind:String, lang:String, entity:String, ids:Seq[JSONID], data:Seq[Json]):Future[Seq[JSONID]] = httpClient.put[Seq[Json],Seq[JSONID]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${JSONID.toMultiString(ids)}"),data)
  def insert(kind:String, lang:String, entity:String, data:Json, public:Boolean): Future[JSONID] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[Json,JSONID](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity"),data)
  }
  def delete(kind:String, lang:String, entity:String, id:JSONID):Future[JSONCount] = httpClient.delete[JSONCount](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${id.asString}"))
  def deleteMany(kind:String, lang:String, entity:String, ids:Seq[JSONID]):Future[JSONCount] = httpClient.delete[JSONCount](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${JSONID.toMultiString(ids)}"))

  //files
  def sendFile(file:File, id:JSONID, entity:String): Future[Int] = httpClient.sendFile[Int](Routes.apiV1(s"/file/$entity/${id.asString}"),file)

  //other utilsString
  def login(request:LoginRequest) = httpClient.post[LoginRequest,Json](Routes.apiV1("/login"),request)
  def logout() = httpClient.get[String](Routes.apiV1("/logout"))
  def labels(lang:String):Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/labels/$lang"))
  def conf():Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/conf"))
  def ui():Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/ui"))
  def news(lang:String):Future[Seq[NewsEntry]] = httpClient.get[Seq[NewsEntry]](Routes.apiV1(s"/news/$lang"))


  //export
  def dataMetadata(kind:String,name:String,lang:String) = httpClient.get[JSONMetadata](Routes.apiV1(s"/$kind/$name/metadata/$lang"))
  def dataDef(kind:String,name:String,lang:String) = httpClient.get[ExportDef](Routes.apiV1(s"/$kind/$name/def/$lang"))
  def dataList(kind:String,lang:String) = httpClient.get[Seq[ExportDef]](Routes.apiV1(s"/$kind/list/$lang"))

  def data(kind:String,name:String,params:Json,lang:String):Future[Seq[Seq[String]]] = httpClient.post[Json,String](Routes.apiV1(s"/$kind/$name/$lang"), params).map{ result =>
    result.asUnsafeCsvReader[Seq[String]](rfc).toSeq
  }

  def tableAccess(table:String, kind:String) = httpClient.get[TableAccess](Routes.apiV1(s"/access/$kind/$table/table-access"))


  override def renderTable(table: PDFTable): Future[String] = httpClient.post[PDFTable,String](Routes.apiV1(s"/renderTable"),table)

  //admin
  def generateStub(entity:String) = httpClient.get[Boolean](Routes.apiV1(s"/create-stub/$entity"))
  override def definition(): Future[BoxDefinition] = httpClient.get[BoxDefinition](Routes.apiV1(s"/box-definition"))
  override def definitionDiff(definition: BoxDefinition): Future[BoxDefinitionMerge] = httpClient.post[BoxDefinition,BoxDefinitionMerge](Routes.apiV1(s"/box-definition/diff"),definition)
  override def definitionCommit(merge: BoxDefinitionMerge): Future[Boolean] = httpClient.post[BoxDefinitionMerge,Boolean](Routes.apiV1(s"/box-definition/commit"),merge)
}
