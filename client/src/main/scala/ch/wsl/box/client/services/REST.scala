package ch.wsl.box.client.services

import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.BoxDefinition
import ch.wsl.box.model.shared._
import ch.wsl.box.model.shared.geo.GeoDataRequest
import ch.wsl.box.model.shared.oidc.UserInfo
import io.circe.Json
import org.scalajs.dom
import org.scalajs.dom.File

import scala.concurrent.{ExecutionContext, Future}



trait REST{
  def version()(implicit ec:ExecutionContext):Future[String]
  def appVersion()(implicit ec:ExecutionContext):Future[String]
  def validSession()(implicit ec:ExecutionContext):Future[Boolean]
  def me()(implicit ec:ExecutionContext):Future[UserInfo]
  def cacheReset()(implicit ec:ExecutionContext):Future[String]
  def entities(kind:String)(implicit ec:ExecutionContext):Future[Seq[String]]

  //for entities and forms
  def specificKind(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext):Future[String]
  def list(kind:String, lang:String, entity:String, limit:Int)(implicit ec:ExecutionContext): Future[Seq[Json]]
  def list(kind:String, lang:String, entity:String, query:JSONQuery)(implicit ec:ExecutionContext): Future[Seq[Json]]

  def geoData(kind:String, lang:String, entity:String, field:String, request:GeoDataRequest,public:Boolean)(implicit ec:ExecutionContext):Future[GeoTypes.GeoData]
  def csv(kind:String, lang:String, entity:String, q:JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[Seq[String]]]
  def count(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext): Future[Int]
  def keys(kind:String, lang:String, entity:String)(implicit ec:ExecutionContext): Future[Seq[String]]
  def ids(kind:String, lang:String, entity:String, q:JSONQuery,public:Boolean)(implicit ec:ExecutionContext): Future[IDs]
  def lookups(kind:String, lang:String, entity:String, fk:JSONLookupsRequest,public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONLookups]]
  def metadata(kind:String, lang:String, entity:String, public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata]
  def tabularMetadata(kind:String, lang:String, entity:String, public:Boolean)(implicit ec:ExecutionContext): Future[JSONMetadata]

  //only for forms
  def children(kind:String, entity:String, lang:String, public:Boolean)(implicit ec:ExecutionContext): Future[Seq[JSONMetadata]]
  def lookup(kind:String, lang:String,entity:String,  field:String, query: JSONQuery,public:Boolean = false)(implicit ec:ExecutionContext): Future[Seq[JSONLookup]]


  //for entities and forms
  def get(kind:String, lang:String, entity:String, id:JSONID,public:Boolean = false)(implicit ec:ExecutionContext):Future[Json]
  def maybeGet(kind:String, lang:String, entity:String, id:JSONID,public:Boolean = false)(implicit ec:ExecutionContext):Future[Option[Json]]
  def update(kind:String, lang:String, entity:String, id:JSONID, data:Json,public:Boolean = false)(implicit ec:ExecutionContext):Future[Json]
  def updateMany(kind:String, lang:String, entity:String, ids:Seq[JSONID], data:Seq[Json])(implicit ec:ExecutionContext):Future[Seq[Json]]
  def insert(kind:String, lang:String, entity:String, data:Json, public:Boolean)(implicit ec:ExecutionContext): Future[Json]
  def delete(kind:String, lang:String, entity:String, id:JSONID)(implicit ec:ExecutionContext):Future[JSONCount]
  def deleteMany(kind:String, lang:String, entity:String, ids:Seq[JSONID])(implicit ec:ExecutionContext):Future[JSONCount]

  //files
  def sendFile(file:File, id:JSONID, entity:String)(implicit ec:ExecutionContext): Future[Int]

  //other utilsString
  def login(request:LoginRequest)(implicit ec:ExecutionContext):Future[UserInfo]
  def authenticate(code:String,provider_id:String)(implicit ec:ExecutionContext):Future[UserInfo]
  def logout()(implicit ec:ExecutionContext):Future[String]
  def labels(lang:String)(implicit ec:ExecutionContext):Future[Map[String,String]]
  def conf()(implicit ec:ExecutionContext):Future[Map[String,String]]
  def ui()(implicit ec:ExecutionContext):Future[Map[String,String]]
  def news(lang:String)(implicit ec:ExecutionContext):Future[Seq[NewsEntry]]


  //export
  def dataMetadata(kind:String,name:String,lang:String)(implicit ec:ExecutionContext):Future[JSONMetadata]
  def dataDef(kind:String,name:String,lang:String)(implicit ec:ExecutionContext):Future[ExportDef]
  def dataList(kind:String,lang:String)(implicit ec:ExecutionContext):Future[Seq[ExportDef]]
  def data(kind:String,name:String,params:Json,lang:String)(implicit ec:ExecutionContext):Future[Seq[Seq[String]]]

  def tableAccess(table:String, kind:String)(implicit ec:ExecutionContext):Future[TableAccess]

  //renderers
  def renderTable(table:PDFTable)(implicit ec:ExecutionContext):Future[String]
  def exportCSV(table:CSVTable)(implicit ec:ExecutionContext):Future[File]
  def exportXLS(table:XLSTable)(implicit ec:ExecutionContext):Future[File]

  //admin
  def generateStub(entity:String)(implicit ec:ExecutionContext):Future[Boolean]
  def definition()(implicit ec:ExecutionContext):Future[BoxDefinition]
  def definitionDiff(definition:BoxDefinition)(implicit ec:ExecutionContext):Future[BoxDefinitionMerge]
  def definitionCommit(merge:BoxDefinitionMerge)(implicit ec:ExecutionContext):Future[Boolean]

  def translationsFields(lang:String)(implicit ec:ExecutionContext):Future[Seq[Field]]
  def translationsFieldsCommit(merge:BoxTranslationsFields)(implicit ec:ExecutionContext):Future[Int]

  def execute(functionName:String,lang:String,data:Json)(implicit ec:ExecutionContext):Future[DataResultTable]
}

