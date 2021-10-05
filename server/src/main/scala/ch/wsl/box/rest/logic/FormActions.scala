package ch.wsl.box.rest.logic

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ch.wsl
import ch.wsl.box
import ch.wsl.box.jdbc
import ch.wsl.box.jdbc.{Connection, FullDatabase, PostgresProfile}
import io.circe.{Json, JsonNumber, _}
import io.circe.syntax._
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.routes.enablers.CSVDownload
import ch.wsl.box.rest.utils.{Timer, UserProfile}
import scribe.Logging
import slick.basic.DatabasePublisher
import slick.lifted.Query
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.html.Html
import ch.wsl.box.rest.metadata.MetadataFactory
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.circe.Json.JNumber

import scala.concurrent.{ExecutionContext, Future}


case class ReferenceKey(localField:String,remoteField:String,value:String)
case class Reference(association:Seq[ReferenceKey])

case class FormActions(metadata:JSONMetadata,
                       jsonActions: String => TableActions[Json],
                       metadataFactory: MetadataFactory
                      )(implicit db:FullDatabase, mat:Materializer, ec:ExecutionContext,services:Services) extends DBFiltersImpl with Logging with TableActions[Json] {

  import ch.wsl.box.shared.utils.JSONUtils._



  val jsonAction = jsonActions(metadata.entity)



  def getById(id:JSONID) = {
    logger.info("Getting Form data")


    jsonAction.getById(id).flatMap{ row =>
      DBIO.sequenceOption(row.map(expandJson))
    }
  }


  private def queryForm(query: JSONQuery):JSONQuery = metadata.query.map{ defaultQuery =>
    JSONQuery(
      filter = defaultQuery.filter ++ query.filter,
      sort = query.sort ++ defaultQuery.sort,
      paging = query.paging.orElse(defaultQuery.paging),
      lang = defaultQuery.lang
    )
  }.getOrElse(query)

  private def streamSeq(query:JSONQuery):DBIO[Seq[Json]] = {

    jsonAction.find(queryForm(query)).flatMap{ rows =>
      DBIO.sequence(rows.map(expandJson))
    }

  }


  private def _list(query:JSONQuery):DBIO[Seq[Json]] = {
    metadata.view.map(v => Registry().actions(v)) match {
      case None => streamSeq(query)
      case Some(v) => v.find(query)
    }
  }

  private def listRenderer(json:Json,lookupElements:Option[Map[String,Seq[Json]]],dropHtml:Boolean = false)(name:String):Json = {

    def jsonCSVRenderer(json:Json,field:JSONField):Json = {

      val format = field.params.flatMap(_.getOpt("format"))

      (field.`type`,field.widget) match {
        case (JSONFieldTypes.DATETIME,_) if format.isDefined => DateTimeFormatters.timestamp.parse(json.get(field.name)) match {
          case Some(value) => DateTimeFormatters.timestamp.format(value,format).asJson
          case None => json.js(field.name)
        }
        case (_,Some(WidgetsNames.integerDecimal2)) => json.js(field.name).withNumber(n => "%.2f".format((n.toDouble / 100.0)).asJson)
        case (_,Some(WidgetsNames.richTextEditorFull)) | (_,Some(WidgetsNames.richTextEditor)) | (_,Some(WidgetsNames.redactor)) if dropHtml => Html.stripTags(json.get(field.name)).asJson
        case (_,_) => json.js(field.name)
      }
    }

    Lookup.valueExtractor(lookupElements, metadata)(name, json.js(name)).map(_.asJson)
      .orElse(metadata.fields.find(_.name == name).map(jsonCSVRenderer(json,_)))
      .getOrElse(json.js(name))
  }



  def list(query:JSONQuery,lookupElements:Option[Map[String,Seq[Json]]],dropHtml:Boolean = false):DBIO[Seq[Json]] = _list(queryForm(query)).map{ _.map{ row =>


    val columns = metadata.tabularFields.map{f =>
      (f, listRenderer(row,lookupElements,dropHtml)(f))
    }
    Json.obj(columns:_*)
  }}

  def csv(query:JSONQuery,lookupElements:Option[Map[String,Seq[Json]]],fields:JSONMetadata => Seq[String] = _.tabularFields):DBIO[CSVTable] = {

    import kantan.csv._
    import kantan.csv.ops._

    _list(queryForm(query)).map { rows =>

      val csvRows = rows.map { json =>


        fields(metadata).map(listRenderer(json, lookupElements)).map(_.string)
      }
      CSVTable(title = metadata.label, header = Seq(), rows = csvRows, showHeader = false)
    }


  }





  /**
   * When default `arrayIndex` is specified the value of the field is substituted with
   * the index of the list. This is useful for keep the sorting of subforms.
   * If no arrayIndex is present is NOOP
   * @param jsonToInsert subforms JSON list
   * @param form subform metadata
   * @return subform JSON list with the sobstitution arrayIndex -> i
   */
  def attachArrayIndex(jsonToInsert:Seq[Json],form:JSONMetadata):Seq[Json] = {
    jsonToInsert.zipWithIndex.map{ case (jsonRow,i) =>
      val values = form.fields.filter(_.default.contains("arrayIndex")).map{ fieldToAdd =>
        fieldToAdd.name -> i
      }.toMap
      jsonRow.deepMerge(values.asJson) //overwrite field value with array index
    }
  }



  def attachParentId(jsonToInsert:Seq[Json],parentJson:Json, child:Child):Seq[Json] = {
    val values = child.mapping.map{ m =>
      m.child -> parentJson.js(m.parent)
    }.toMap

    jsonToInsert.map{ jsonRow =>
      jsonRow.deepMerge(values.asJson) //overwrite field value with array index
    }
  }

  def subAction[T](e:Json, action: FormActions => ((Option[JSONID],Json) => DBIO[_]),alwaysApply:Boolean = false): Seq[DBIO[Seq[_]]] = metadata.fields.filter(_.child.isDefined).filter { field =>
    field.condition match {
      case Some(value) => alwaysApply || value.conditionValues.contains(e.js(value.conditionFieldId))
      case None => true
    }
  }.map{ field =>
    for {
      form <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(field.child.get.objId, metadata.lang)))
      dbSubforms <- getChild(e,form,field.child.get)
      subs = e.seq(field.name)
      subJsonWithIndexs = attachArrayIndex(subs,form)
      subJson = attachParentId(subJsonWithIndexs,e,field.child.get)
      deleted <- DBIO.sequence(deleteChild(form,subJson,dbSubforms))
      result <- DBIO.sequence(subJson.map{ json => //order matters so we do it synchro
          action(FormActions(form,jsonActions,metadataFactory))(json.ID(form.keys),json).map(x => Some(x))
      }).map(_.flatten)
    } yield result
  }

  def deleteSingle(id: JSONID,e:Json) = {
    jsonAction.delete(id)
  }


  /**
   * Delete child if removed from parent JSON
   * @param child child form metadata
   * @param receivedJson list of child recived
   * @param dbJson list of child present in DB
   * @return number of deleted rows
   */
  def deleteChild(child:JSONMetadata, receivedJson:Seq[Json], dbJson:Seq[Json]): Seq[DBIO[Int]] = {
    val receivedID = receivedJson.flatMap(_.ID(child.keys))
    val dbID = dbJson.flatMap(_.ID(child.keys))
    logger.debug(s"child: ${child.name} received: ${receivedID.map(_.asString)} db: ${dbID.map(_.asString)}")
    dbID.filterNot(k => receivedID.contains(k)).map{ idsToDelete =>
      logger.info(s"Deleting child ${child.name}, with key: $idsToDelete")
      jsonActions(child.entity).delete(idsToDelete)
    }
  }


  def delete(id:JSONID) = {
    for{
      json <- getById(id)
      subs <- DBIO.sequence(subAction(json.get,x => (id,json) => x.deleteSingle(id.get,json),true))
      current <- deleteSingle(id,json.get)
    } yield current + subs.size
  }

  def insert(e:Json) = for{
    inserted <- jsonAction.insertReturningModel(e)
    _ <- DBIO.sequence(metadata.fields.filter(_.child.isDefined).map { field =>
      for {
        metadata <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(field.child.get.objId, metadata.lang)))
        rows = attachArrayIndex(e.seq(field.name),metadata)
        //attach parent id
        rowsWithId = rows.map{ row =>
          field.child.get.mapping.foldLeft(row){ case (acc,m) => acc.deepMerge(Json.obj(m.child -> inserted.js(m.parent)))}
        }
        result <- DBIO.sequence(rowsWithId.map(row => FormActions(metadata,jsonActions,metadataFactory).insert(row)))
      } yield result
    })
  } yield JSONID.fromData(inserted,metadata).getOrElse(JSONID.empty)


  override def insertReturningModel(obj: Json) = for{
    id <- insert(obj)
    full <- getById(id)
  } yield full.getOrElse(Json.Null)

  private def insertNullForMissingFields(json:Json):Json = {
    val allNullFields = Json.fromFields(metadata.fields.filter(_.isDbStored).map(x => x.name -> Json.Null))
    allNullFields.deepMerge(json)
  }

  def update(id:JSONID, e:Json) = {
    for{
      _ <- DBIO.sequence(subAction(e,_.upsertIfNeeded))  //need upsert to add new child records
      result <- jsonAction.update(id,insertNullForMissingFields(e))
    } yield result
  }

  def updateIfNeeded(id:JSONID, e:Json) = {

    for{
      _ <- DBIO.sequence(subAction(e,_.upsertIfNeeded))  //need upsert to add new child records
      result <- jsonAction.updateIfNeeded(id,insertNullForMissingFields(e))
    } yield result
  }

  def upsertIfNeeded(id:Option[JSONID], json: Json):DBIO[JSONID] = {
    for {
      current <- id match {
        case Some(id) => getById(id)
        case None => DBIO.successful(None)
      } //retrieve values in db
      result <- if (current.isDefined) { //if exists, check if we have to skip the update (if row is the same)
        update(id.get, current.get.deepMerge(insertNullForMissingFields(json))).map(_ => id.get)
      } else {
        insert(json)
      }
    } yield {
      logger.info(s"Inserted $result")
      result
    }
  }

  private def createQuery(entity:Json, child: Child):JSONQuery = {
    val parentFilter = for{
      m <- child.mapping
    } yield {
      JSONQueryFilter(m.child,Some(Filter.EQUALS),entity.get(m.parent))
    }

    val filters = parentFilter ++ child.childQuery.toSeq.flatMap(_.filter)



    child.childQuery.getOrElse(JSONQuery.empty).copy(filter=filters.toList.distinct)
  }

  private def getChild(dataJson:Json, metadata:JSONMetadata, child:Child):DBIO[Seq[Json]] = {
    val query = createQuery(dataJson,child)
    FormActions(metadata,jsonActions,metadataFactory).streamSeq(query)
  }

  private def expandJson(dataJson:Json):DBIO[Json] = {

    val values = metadata.fields.map{ field =>
      {(field.`type`,field.child) match {
        case ("static",_) => DBIO.successful(field.name -> field.default.asJson)  //set default value
        case (_,None) => DBIO.successful(field.name -> dataJson.js(field.name))        //use given value
        case (_,Some(child)) => for{
          form <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(child.objId,metadata.lang)))
          data <- getChild(dataJson,form,child)
        } yield {
          logger.info(s"expanding child ${field.name} : ${data.asJson}")
          field.name -> data.asJson
        }
      }}
    }
    DBIO.sequence(values).map(_.toMap.asJson)
  }


  override def find(query: JSONQuery) = jsonAction.find(queryForm(query))

  override def count() = metadata.query match {
    case Some(value) => jsonAction.count(value).map(JSONCount)
    case None => jsonAction.count()
  }
  override def count(query: JSONQuery) = jsonAction.count(query)

  override def ids(query: JSONQuery): DBIO[IDs] = {
    val q = queryForm(query)
    val fut:DBIO[(Seq[Json],Int)] = metadata.view.map(v => Registry().actions(v)) match {
      case None => for {
        data <- jsonAction.find(q)
        n <- jsonAction.count(q)
      } yield (data, n)
      case Some(v) => for {
        data <- v.find(q)
        n <- v.count(q)
      } yield (data, n)
    }

    fut.map { case (data:Seq[Json], n:Int) =>
      val last = q.paging match {
        case None => true
        case Some(paging) => (paging.currentPage * paging.pageLength) >= n
      }
      IDs(
        last,
        q.paging.map(_.currentPage).getOrElse(1),
        data.flatMap{ x => JSONID.fromData(x, metadata).map(_.asString) },
        n
      )
    }


  }
}
