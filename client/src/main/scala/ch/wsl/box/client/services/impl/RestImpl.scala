package ch.wsl.box.client.services.impl

import ch.wsl.box.client.Context
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{HttpClient, REST}
import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.BoxDefinition
import ch.wsl.box.model.shared.geo.GeoDataRequest
import ch.wsl.box.model.shared.{BoxTranslationsFields, CSVTable, CurrentUser, DataResultTable, EntityKind, ExportDef, Field, GeoJson, GeoTypes, IDs, JSONCount, JSONFieldMap, JSONID, JSONLookup, JSONLookups, JSONLookupsRequest, JSONMetadata, JSONQuery, LoginRequest, NewsEntry, PDFTable, TableAccess, XLSTable}
import io.circe.{Decoder, Encoder, Json}
import kantan.csv.rfc
import kantan.csv._
import kantan.csv.ops._
import org.scalajs.dom.File
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

class RestImpl(httpClient:HttpClient) extends REST with Logging {



  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.Formatters._
  import ch.wsl.box.model.shared.EntityKind._

  def version()(implicit ec:ExecutionContext) = httpClient.get[String](Routes.apiV1("/version"))
  def appVersion()(implicit ec:ExecutionContext) = httpClient.get[String](Routes.apiV1("/app_version"))
  def validSession()(implicit ec:ExecutionContext) = httpClient.get[Boolean](Routes.apiV1("/validSession"))
  def me()(implicit ec:ExecutionContext) = httpClient.get[CurrentUser](Routes.apiV1("/me"))
  def cacheReset()(implicit ec:ExecutionContext) = httpClient.get[String](Routes.apiV1("/cache/reset"))

  def entities(kind:String)(implicit ec:ExecutionContext):Future[Seq[String]] = httpClient.get[Seq[String]](Routes.apiV1(s"/${EntityKind(kind).plural}"))

  //for entities and forms
  def specificKind(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext):Future[String] = httpClient.get[String](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/kind") )    //distinguish entities into table or view
  def list(kind:String, lang:String, entity:String, limit:Int)(implicit ec:ExecutionContext): Future[Seq[Json]] = httpClient.post[JSONQuery,Seq[Json]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/list"),JSONQuery.empty.limit(limit))
  def list(kind:String, lang:String, entity:String, query:JSONQuery)(implicit ec:ExecutionContext): Future[Seq[Json]] = httpClient.post[JSONQuery,Seq[Json]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/list"),query)
  def csv(kind:String, lang:String, entity:String, q:JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[Seq[String]]] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[JSONQuery,String](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/csv"),q).map{ result =>
      result.asUnsafeCsvReader[Seq[String]](rfc).toSeq
    }
  }
  override def geoData(kind:String, lang:String, entity:String, field:String, request:GeoDataRequest,public:Boolean)(implicit ec:ExecutionContext): Future[GeoTypes.GeoData] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[GeoDataRequest,GeoTypes.GeoData](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/geo-data/$field"),request)
  }

  def count(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext): Future[Int] = httpClient.get[Int](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/count"))
  def keys(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext): Future[Seq[String]] = httpClient.get[Seq[String]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/keys"))
  def ids(kind:String, lang:String, entity:String, q:JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[IDs] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[JSONQuery,IDs](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/ids"),q)
  }
  def metadata(kind:String, lang:String, entity:String, public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata] = {
    val prefix = if(public) "/public" else ""
    httpClient.get[JSONMetadata](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/metadata"))
  }
  def tabularMetadata(kind:String, lang:String, entity:String, public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata] = {
    val prefix = if(public) "/public" else ""
    httpClient.get[JSONMetadata](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/tabularMetadata"))
  }

  //only for forms
  def children(kind:String, entity:String, lang:String, public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONMetadata]] = {
    val prefix = if(public) "/public" else ""
    httpClient.get[Seq[JSONMetadata]](Routes.apiV1(s"$prefix/$kind/$lang/$entity/children"))
  }
  def lookup(kind:String, lang:String,entity:String, field:String, query: JSONQuery, public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONLookup]] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[JSONQuery, Seq[JSONLookup]](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/lookup/$field"), query)
  }


  override def lookups(kind: String, lang: String, entity: String, fk: JSONLookupsRequest,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONLookups]] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[JSONLookupsRequest,Seq[JSONLookups]](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/lookups"),fk)
  }

  private def prefix(kind: String, lang: String, public: Boolean):String = (public, EntityKind(kind).isEntity) match {
    case (true, true) => "/public"
    case (true, false) => s"/public/${EntityKind(kind).entityOrForm}/$lang"
    case (false, _) => s"/${EntityKind(kind).entityOrForm}/$lang"
  }

  //for entities and forms
  override def get(kind:String, lang:String, entity:String, id:JSONID, public:Boolean)(implicit ec:ExecutionContext):Future[Json] = {
    httpClient.get[Json](Routes.apiV1(s"${prefix(kind,lang,public)}/$entity/id/${id.asString}"))
  }

  override def maybeGet(kind: String, lang: String, entity: String, id: JSONID, public: Boolean)(implicit ec:ExecutionContext): Future[Option[Json]] = {
    httpClient.maybeGet[Json](Routes.apiV1(s"${prefix(kind, lang, public)}/$entity/id/${id.asString}"))
  }

  def update(kind:String, lang:String, entity:String, id:JSONID, data:Json, public:Boolean)(implicit ec:ExecutionContext):Future[Json] = {
    val prefix = if(public) "/public" else ""
    httpClient.put[Json,Json](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${id.asString}"),data)
  }
  def updateMany(kind:String, lang:String, entity:String, ids:Seq[JSONID], data:Seq[Json])(implicit ec:ExecutionContext):Future[Seq[Json]] = httpClient.put[Seq[Json],Seq[Json]](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${JSONID.toMultiString(ids)}"),data)
  def insert(kind:String, lang:String, entity:String, data:Json, public:Boolean)(implicit ec:ExecutionContext): Future[Json] = {
    val prefix = if(public) "/public" else ""
    httpClient.post[Json,Json](Routes.apiV1(s"$prefix/${EntityKind(kind).entityOrForm}/$lang/$entity"),data)
  }
  def delete(kind:String, lang:String, entity:String, id:JSONID)(implicit ec:ExecutionContext):Future[JSONCount] = httpClient.delete[JSONCount](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${id.asString}"))
  def deleteMany(kind:String, lang:String, entity:String, ids:Seq[JSONID])(implicit ec:ExecutionContext):Future[JSONCount] = httpClient.delete[JSONCount](Routes.apiV1(s"/${EntityKind(kind).entityOrForm}/$lang/$entity/id/${JSONID.toMultiString(ids)}"))

  //files
  def sendFile(file:File, id:JSONID, entity:String)(implicit ec:ExecutionContext): Future[Int] = httpClient.sendFile[Int](Routes.apiV1(s"/file/$entity/${id.asString}"),file)

  //other utilsString
  def login(request:LoginRequest)(implicit ec:ExecutionContext) = httpClient.post[LoginRequest,Json](Routes.apiV1("/login"),request)
  def logout()(implicit ec:ExecutionContext) = httpClient.get[String](Routes.apiV1("/logout"))
  def labels(lang:String)(implicit ec:ExecutionContext):Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/labels/$lang"))
  def conf()(implicit ec:ExecutionContext):Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/conf"))
  def ui()(implicit ec:ExecutionContext):Future[Map[String,String]] = httpClient.get[Map[String,String]](Routes.apiV1(s"/ui"))
  def news(lang:String)(implicit ec:ExecutionContext):Future[Seq[NewsEntry]] = httpClient.get[Seq[NewsEntry]](Routes.apiV1(s"/news/$lang"))


  //export
  def dataMetadata(kind:String,name:String,lang:String)(implicit ec:ExecutionContext) = httpClient.get[JSONMetadata](Routes.apiV1(s"/$kind/$lang/$name/metadata"))
  def dataDef(kind:String,name:String,lang:String)(implicit ec:ExecutionContext) = httpClient.get[ExportDef](Routes.apiV1(s"/$kind/$lang/$name/def"))
  def dataList(kind:String,lang:String)(implicit ec:ExecutionContext) = httpClient.get[Seq[ExportDef]](Routes.apiV1(s"/$kind/$lang/list"))

  def data(kind:String,name:String,params:Json,lang:String)(implicit ec:ExecutionContext):Future[Seq[Seq[String]]] = httpClient.post[Json,String](Routes.apiV1(s"/$kind/$lang/$name"), params).map{ result =>
    result.asUnsafeCsvReader[Seq[String]](rfc).toSeq
  }

  def tableAccess(table:String, kind:String)(implicit ec:ExecutionContext) = httpClient.get[TableAccess](Routes.apiV1(s"/access/$kind/$table/table-access"))


  override def renderTable(table: PDFTable)(implicit ec:ExecutionContext): Future[String] = httpClient.post[PDFTable,String](Routes.apiV1(s"/renderTable"),table)
  override def exportCSV(table: CSVTable)(implicit ec:ExecutionContext): Future[File] = httpClient.postFileResponse[CSVTable](Routes.apiV1(s"/exportCSV"),table)
  override def exportXLS(table: XLSTable)(implicit ec:ExecutionContext): Future[File] = httpClient.postFileResponse[XLSTable](Routes.apiV1(s"/exportXLS"),table)

  override def execute(functionName: String, lang: String, data:Json)(implicit ec:ExecutionContext): Future[DataResultTable] = httpClient.post[Json,DataResultTable](Routes.apiV1(s"/function/$lang/$functionName/raw"),data)

  //admin
  def generateStub(entity:String)(implicit ec:ExecutionContext) = httpClient.get[Boolean](Routes.apiV1(s"/create-stub/$entity"))
  override def definition()(implicit ec:ExecutionContext): Future[BoxDefinition] = httpClient.get[BoxDefinition](Routes.apiV1(s"/box-definition"))
  override def definitionDiff(definition: BoxDefinition)(implicit ec:ExecutionContext): Future[BoxDefinitionMerge] = httpClient.post[BoxDefinition,BoxDefinitionMerge](Routes.apiV1(s"/box-definition/diff"),definition)
  override def definitionCommit(merge: BoxDefinitionMerge)(implicit ec:ExecutionContext): Future[Boolean] = httpClient.post[BoxDefinitionMerge,Boolean](Routes.apiV1(s"/box-definition/commit"),merge)


  override def translationsFields(lang: String)(implicit ec:ExecutionContext): Future[Seq[Field]] = httpClient.get[Seq[Field]](Routes.apiV1(s"/translations/fields/$lang"))
  override def translationsFieldsCommit(merge: BoxTranslationsFields)(implicit ec:ExecutionContext): Future[Int] = httpClient.post[BoxTranslationsFields,Int](Routes.apiV1(s"/translations/fields/commit"),merge)


}
