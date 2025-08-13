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
import ch.wsl.box.rest.utils.{BoxSession, Timer, UserProfile}
import scribe.Logging
import slick.basic.DatabasePublisher
import slick.lifted.Query
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.GeoJson.{Feature, FeatureCollection, Geometry}
import ch.wsl.box.rest.html.Html
import ch.wsl.box.rest.metadata.MetadataFactory
import ch.wsl.box.rest.runtime.{Registry, RegistryInstance}
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.circe.Json.{JNumber, Null}

import scala.concurrent.{ExecutionContext, Future}


case class FormActions(metadata:JSONMetadata,
                       registry: RegistryInstance,
                       metadataFactory: MetadataFactory
                      )(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext,val services:Services) extends DBFiltersImpl with Logging with TableActions[Json] {

  import ch.wsl.box.shared.utils.JSONUtils._



  val jsonAction = registry.actions(metadata.entity)
  val fkTransform = new FKFilterTransfrom(Registry())



  def getById(id:JSONID) = {
    logger.info("Getting Form data")


    jsonAction.getById(id).flatMap{ row =>
      DBIO.sequenceOption(row.map(expandJson))
    }
  }



  private def queryForm(query: JSONQuery):DBIO[JSONQuery] = {
    val base = metadata.query.map{ defaultQuery =>
      JSONQuery(
        filter = defaultQuery.filter ++ query.filter,
        sort = query.sort ++ defaultQuery.sort,
        paging = query.paging
      )
    }.getOrElse(query)
    fkTransform.preFilter(metadata,base.filter).map{ fil => base.copy(filter = fil.filters.toList)}
  }


  private def streamSeq(query:JSONQuery):DBIO[Seq[Json]] = {

    for{
      q <- queryForm(query)
      rows <- jsonAction.findSimple(q)
      result <- DBIO.sequence(rows.map(expandJson))
    } yield result

  }


  override def distinctOn(fields: Seq[String], query: JSONQuery): DBIO[Seq[Json]] = jsonAction.distinctOn(fields, query)

  override def findSimple(query:JSONQuery): DBIO[Seq[Json]] = {

    for{
      q <- queryForm(query)
      rows <- jsonAction.findSimple(q)
      result <- DBIO.sequence(rows.map(expandJson))
    } yield result

  }


  override def fetchFields(fields: Seq[String], query: JSONQuery): DBIO[Seq[Json]] = for{
    q <- queryForm(query)
    result <- jsonAction.fetchFields(fields,q)
  } yield result

  override def fetchGeom(properties:Seq[String],field:String,query:JSONQuery):DBIO[Seq[(Json,Geometry)]] = for {
    q <- queryForm(query)
    result <- jsonAction.fetchGeom(properties,field, q)
  } yield result

  private def _list(query:JSONQuery):DBIO[Seq[Json]] = {
    queryForm(query).flatMap { q =>
      metadata.view.map(v => Registry().actions(v)) match {
        case None => streamSeq(q)
        case Some(v) => v.findSimple(q)
      }
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





  def dataTable(query:JSONQuery,lookupElements:Option[Map[String,Seq[Json]]],dropHtml:Boolean = false) = _list(query).map{ rows =>

    val fields = metadata.exportFields.flatMap(f => metadata.fields.find(_.name == f))

    val data: Seq[Seq[Json]] = rows.map { row =>
      fields.map { f =>
        listRenderer(row, lookupElements, dropHtml)(f.name)
      }
    }
    val keys = rows.map(row => JSONID.fromBoxObjectId(row,metadata).map(_.asString))

    val geomColumn = fields.filter(_.`type` == JSONFieldTypes.GEOMETRY)
    DataResultTable(fields.map(_.title),fields.map(_.`type`),data,keys,geomColumn.map{ case f =>
      f.name -> rows.map{ row => row.js(f.name).as[Geometry].toOption }
    }.toMap)
  }

  private def fkDataComplete(tabularFieldsKey:Seq[String], rows:Seq[Json]): DBIO[Option[Seq[(String, Seq[Json])]]] = {
    val tabularFields = metadata.fields.filter(x => tabularFieldsKey.contains(x.name))
    val lookupFields = tabularFields.filter(_.remoteLookup.isDefined)

    DBIO.sequence(lookupFields.map{ lf =>
      val localFields = lf.remoteLookup.get.map.localKeysColumn
      val foreignFields = lf.remoteLookup.get.map.foreign.keyColumns


      val data: Seq[Seq[String]] = rows.map(r => localFields.map(f => r.get(f))).filterNot(_.forall(_ == "")).transpose.map(_.distinct)
      val filters = data.zip(foreignFields).map{ case (d,ff) => JSONQueryFilter.WHERE.in(ff,d)}
      val fkQuery = JSONQuery.filterWith(filters:_*).limit(100000)
      Registry().actions(lf.remoteLookup.get.lookupEntity).findSimple(fkQuery).map{ fk =>
        lf.remoteLookup.get.lookupEntity -> fk
      }
    }).map(x => Some(x))
  }

  private def noFkData = DBIO.successful(None)

  override def lookups(request: JSONLookupsRequest): DBIO[Seq[JSONLookups]] = {
    for{
      result <- DBIO.sequence(request.fields.map(r => fkTransform.singleLookup(metadata,r, request.query)))
    } yield result
  }


  private def __list(query:JSONQuery,resolveLookup:Boolean = false,dropHtml:Boolean = false,fields:JSONMetadata => Seq[String] = _.tabularFields):DBIO[Seq[Seq[(String,Json)]]] = {

    _list(query).flatMap{ rows =>
      val fkData = if(resolveLookup) fkDataComplete(fields(metadata),rows) else noFkData

      fkData.map { lookupElements =>
        rows.map { row =>
          val columns = fields(metadata).map { f =>
            (f, listRenderer(row, lookupElements.map(_.toMap), dropHtml)(f))
          }

          columns
        }
      }
    }
  }

  def list(query:JSONQuery,resolveLookup:Boolean = false,dropHtml:Boolean = false,fields:JSONMetadata => Seq[String] = _.tabularFields):DBIO[Seq[Json]] = __list(query,resolveLookup, dropHtml).map{ rows =>
    rows.map(Json.fromFields)
  }


  def csv(query:JSONQuery,resolveLookup:Boolean = false,fields:JSONMetadata => Seq[String] = _.tabularFields):DBIO[CSVTable] = {

    __list(query,resolveLookup,false,fields).map{ rows =>
        CSVTable(title = metadata.label, header = Seq(), rows = rows.map(_.map(_._2.string)), showHeader = false)
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

  def attachEmptyFields(form: JSONMetadata)(jsonToUpdate: Json): Json = {

    val emptyFields = form.fields.flatMap {f =>
      (f.child,f.condition) match {
        case (Some(child),Some(condition)) if child.hasData && Condition.check(condition,jsonToUpdate) => {
          Some(f.name -> Json.fromValues(Seq()))
        }
        case _ => None
      }
    }

    //jsonToUpdate.deepMerge(Json.fromFields(emptyFields))

    Json.fromFields(emptyFields).deepMerge(jsonToUpdate)
  }



  def attachParentId(jsonToInsert:Seq[Json],parentJson:Json, child:Child):Seq[Json] = {
    val values = child.mapping.map{ m =>
      m.child -> parentJson.js(m.parent)
    }.toMap

    jsonToInsert.map{ jsonRow =>
      jsonRow.deepMerge(values.asJson) //overwrite field value with array index
    }
  }

  private def jsonIdDBFields(metadata:JSONMetadata,fields:Seq[JSONField]):Seq[JSONField] = fields.map{ field =>
    val newField = registry.fields.field(metadata.entity, field.name) match {
      case Some(col) => {
        if(col.nullable != field.nullable)
          field.copy(nullable = col.nullable)
        else field
      }
      case _ => field
    }
    newField
  }

  def subAction[T](e:Json, action: FormActions => ((Option[JSONID],Json) => DBIO[Json])): DBIO[Seq[(String,Json)]] = {

    logger.debug(s"Applying sub action to $e")


    val subFields = metadata.fields
      .filter(f => f.child.exists(_.hasData) && !f.readOnly)
      .filter{f =>
        f.condition.map(Condition.check(_,e)) match {
          case Some(true) => true
          case Some(false) => f.params.exists(_.js("deleteWhenHidden") == Json.True)
          case None => true
        }
      }

    val result = subFields.map{ field =>
      for {
        form <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(field.child.get.objId, metadata.lang,session.user)))
        dbSubforms <- getChild(e,form,field.child.get)
        subs =  e.seq(field.name)
        subJsonWithIndexs = attachArrayIndex(subs,form)
        subJsonWithNegativeConditionFields = subJsonWithIndexs.map(attachEmptyFields(form))
        subJson = attachParentId(subJsonWithNegativeConditionFields,e,field.child.get)
         deleted <- if(field.params.exists(_.js("avoidDelete") == Json.True)) DBIO.successful(0) else DBIO.sequence(deleteChild(form,subJson,dbSubforms))
        result <- DBIO.sequence(subJson.map{ json => //order matters so we do it synchro
          val id = json.ID(jsonIdDBFields(form,form.keyFields))
          action(FormActions(form,registry,metadataFactory))(id,json)
        })
      } yield field.name -> result.asJson
    }
    DBIO.sequence(result)
  }

  def deleteSingle(id: JSONID,e:Json) = {
    jsonAction.delete(id).map(_ => e)
  }


  /**
   * Delete child if removed from parent JSON
   * @param child child form metadata
   * @param receivedJson list of child recived
   * @param dbJson list of child present in DB
   * @return number of deleted rows
   */
  def deleteChild(child:JSONMetadata, receivedJson:Seq[Json], dbJson:Seq[Json]): Seq[DBIO[Int]] = {
    val receivedID = receivedJson.flatMap(_.ID(jsonIdDBFields(child,child.keyFields)))
    val dbID = dbJson.flatMap(_.ID(jsonIdDBFields(child,child.keyFields)))
    logger.debug(s"child: ${child.name} received: ${receivedID.map(_.asString)} db: ${dbID.map(_.asString)}")
    dbID.filterNot(k => receivedID.contains(k)).map{ idsToDelete =>
      logger.info(s"Deleting child ${child.name}, with key: $idsToDelete")
      registry.actions(child.entity).delete(idsToDelete)
    }
  }


  def delete(id:JSONID) = {
    for{
      json <- getById(id)
      subs <- subAction(json.get,x => (id,json) => x.deleteSingle(id.get,json))
      current <- deleteSingle(id,json.get)
    } yield 1 + subs.length
  }

  def insert(e:Json):DBIO[Json] = for{
    inserted <- jsonAction.insert(e)
    childs <- DBIO.sequence(metadata.fields.filter(_.child.isDefined).map { field =>
      for {
        metadata <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(field.child.get.objId, metadata.lang,session.user)))
        rows = attachArrayIndex(e.seq(field.name),metadata)
        //attach parent id
        rowsWithId = rows.map{ row =>
          field.child.get.mapping.foldLeft(row){ case (acc,m) => acc.deepMerge(Json.obj(m.child -> inserted.js(m.parent)))}
        }
        result <- DBIO.sequence(rowsWithId.map(row => FormActions(metadata,registry,metadataFactory).insert(row)))
      } yield field.name -> result.asJson
    })
  } yield inserted.deepMerge(Json.fromFields(childs))


  private def dbTableFields:Set[String] = registry.fields.tableFields(metadata.entity).keySet

  private def insertNullForMissingFields(json:Json):Json = {
    val allNullFields = Json.fromFields(metadata.fields.filter(_.isDbStored(dbTableFields)).map(x => x.name -> Json.Null))
    allNullFields.deepMerge(json)
  }

  private def updateOneLevel(id:JSONID, e:Json):DBIO[Json] = {
    logger.info(s"UPDATE BY ID $id")

    val dataWithoutChilds = e.filterFields(metadata.fields.filter(f => f.isDbStored(dbTableFields) && f.`type` != JSONFieldTypes.CHILD))

    for {
      current <- jsonAction.getById(id)
      newRow <- current match {
        case Some(value) => DBIO.successful(value)
        case None => jsonAction.insert(dataWithoutChilds)
      }
      diff = newRow.diff(JSONMetadata.layoutOnly(metadata), Seq())(dataWithoutChilds)
      result <- jsonAction.updateDiff(diff)
    } yield result.orElse(current).getOrElse(dataWithoutChilds)
  }

  def update(id:JSONID, e:Json):DBIO[Json] = {

    val _mapData = e.filterFields(metadata.fields.filter(_.map.isDefined))
    val mapData =  _mapData.asObject.toList.flatMap(_.values).flatMap(_.as[FeatureCollection].toOption)

    for{
      result <- updateOneLevel(id,insertNullForMissingFields(e))
      childs <- subAction(e.deepMerge(result),_.upsertIfNeeded)  //need upsert to add new child records, deepMerging result in case of trigger data modification
      _ <- DBIO.sequence(mapData.flatMap(_.features).map(MapActions.save))
    } yield result
      .filterFields(metadata.fields) // don't expose data not contained in the current form
      .deepMerge(Json.fromFields(childs))
  }

  def upsertIfNeeded(id:Option[JSONID], json: Json):DBIO[Json] = {
    for {
      current <- id match {
        case Some(id) => getById(id)
        case None => DBIO.successful(None)
      } //retrieve values in db
      result <- if (current.isDefined) { //if exists, check if we have to skip the update (if row is the same)
        update(id.get, current.get.deepMerge(insertNullForMissingFields(json)))
      } else {
        insert(json)
      }
    } yield {
      logger.info(s"Inserted $result")
      result
    }
  }




  override def updateDiff(diff: JSONDiff):DBIO[Option[Json]] = ???

  private def createQuery(entity:Json, child: Child):JSONQuery = {
    val parentFilter = for{
      m <- child.mapping
    } yield {
      JSONQueryFilter.withValue(m.child,Some(Filter.EQUALS),entity.get(m.parent))
    }

    val filters = parentFilter ++ child.childQuery.toSeq.flatMap(_.filter)



    child.childQuery.getOrElse(JSONQuery.empty).copy(filter=filters.toList.distinct)
  }

  private def getChild(dataJson:Json, metadata:JSONMetadata, child:Child):DBIO[Seq[Json]] = {
    val query = createQuery(dataJson,child)
    FormActions(metadata,registry,metadataFactory).findSimple(query)
  }

  private def expandJson(dataJson:Json):DBIO[Json] = {

    val values = metadata.fields.map{ field =>
      {(field.`type`,field.child) match {
        case ("static",_) => DBIO.successful(field.name -> field.default.asJson)  //set default value
        case (_,None) => DBIO.successful(field.name -> dataJson.js(field.name))        //use given value
        case (_,Some(child)) if child.hasData => for{
          form <- DBIO.from(services.connection.adminDB.run(metadataFactory.of(child.objId,metadata.lang,session.user)))
          data <- getChild(dataJson,form,child)
        } yield {
          logger.info(s"expanding child ${field.name} : ${data.asJson}")
          field.name -> data.asJson
        }
        case (_,_) => DBIO.successful(field.name -> Json.Null)
      }}
    }
    DBIO.sequence(values).map(x => JSONID.attachBoxObjectId(x.toMap.asJson,metadata.keys))
  }




  override def count() = metadata.query match {
    case Some(value) => jsonAction.count(value).map(JSONCount)
    case None => jsonAction.count()
  }
  override def count(query: JSONQuery) = jsonAction.count(query)

  override def ids(query: JSONQuery,keys:Seq[String]): DBIO[IDs] = for{
    q <- queryForm(query)
    result <- registry.actions(metadata.view.getOrElse(metadata.entity)).ids(q,keys)
  } yield result
}
