package ch.wsl.box.client.services

import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.BoxDefinition
import ch.wsl.box.model.shared._
import io.circe.Json
import org.scalajs.dom
import org.scalajs.dom.File

import scala.concurrent.Future



trait REST{
  def version():Future[String]
  def appVersion():Future[String]
  def validSession():Future[Boolean]
  def me():Future[String]
  def cacheReset():Future[String]
  def entities(kind:String):Future[Seq[String]]

  //for entities and forms
  def specificKind(kind:String, lang:String, entity:String):Future[String]
  def list(kind:String, lang:String, entity:String, limit:Int): Future[Seq[Json]]
  def list(kind:String, lang:String, entity:String, query:JSONQuery): Future[Seq[Json]]
  def csv(kind:String, lang:String, entity:String, q:JSONQuery): Future[Seq[Seq[String]]]
  def count(kind:String, lang:String, entity:String): Future[Int]
  def keys(kind:String, lang:String, entity:String): Future[Seq[String]]
  def ids(kind:String, lang:String, entity:String, q:JSONQuery): Future[IDs]
  def lookups(kind:String, lang:String, entity:String, fk:JSONLookupsRequest): Future[Seq[JSONLookups]]
  def metadata(kind:String, lang:String, entity:String, public:Boolean): Future[JSONMetadata]
  def tabularMetadata(kind:String, lang:String, entity:String): Future[JSONMetadata]

  //only for forms
  def children(kind:String, entity:String, lang:String, public:Boolean): Future[Seq[JSONMetadata]]
  def lookup(kind:String, lang:String,entity:String,  field:String, query: JSONQuery,public:Boolean = false): Future[Seq[JSONLookup]]


  //for entities and forms
  def get(kind:String, lang:String, entity:String, id:JSONID,public:Boolean = false):Future[Json]
  def maybeGet(kind:String, lang:String, entity:String, id:JSONID,public:Boolean = false):Future[Option[Json]]
  def update(kind:String, lang:String, entity:String, id:JSONID, data:Json,public:Boolean = false):Future[Json]
  def updateMany(kind:String, lang:String, entity:String, ids:Seq[JSONID], data:Seq[Json]):Future[Seq[Json]]
  def insert(kind:String, lang:String, entity:String, data:Json, public:Boolean): Future[Json]
  def delete(kind:String, lang:String, entity:String, id:JSONID):Future[JSONCount]
  def deleteMany(kind:String, lang:String, entity:String, ids:Seq[JSONID]):Future[JSONCount]

  //files
  def sendFile(file:File, id:JSONID, entity:String): Future[Int]

  //other utilsString
  def login(request:LoginRequest):Future[Json]
  def logout():Future[String]
  def labels(lang:String):Future[Map[String,String]]
  def conf():Future[Map[String,String]]
  def ui():Future[Map[String,String]]
  def news(lang:String):Future[Seq[NewsEntry]]


  //export
  def dataMetadata(kind:String,name:String,lang:String):Future[JSONMetadata]
  def dataDef(kind:String,name:String,lang:String):Future[ExportDef]
  def dataList(kind:String,lang:String):Future[Seq[ExportDef]]
  def data(kind:String,name:String,params:Json,lang:String):Future[Seq[Seq[String]]]

  def tableAccess(table:String, kind:String):Future[TableAccess]

  //renderers
  def renderTable(table:PDFTable):Future[String]
  def exportCSV(table:CSVTable):Future[File]
  def exportXLS(table:XLSTable):Future[File]

  //admin
  def generateStub(entity:String):Future[Boolean]
  def definition():Future[BoxDefinition]
  def definitionDiff(definition:BoxDefinition):Future[BoxDefinitionMerge]
  def definitionCommit(merge:BoxDefinitionMerge):Future[Boolean]

  def translationsFields(lang:String):Future[Seq[Field]]
  def translationsFieldsCommit(merge:BoxTranslationsFields):Future[Int]

  def execute(functionName:String,lang:String,data:Json):Future[DataResultTable]
}

