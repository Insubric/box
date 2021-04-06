package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.information_schema.{PgColumn, PgColumns, PgInformationSchema}
import ch.wsl.box.jdbc.{Connection, FullDatabase, Managed, UserDatabase}
import ch.wsl.box.model.boxentities.BoxField.{BoxFieldFile_row, BoxField_i18n_row, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_i18nTable, BoxForm_row}
import ch.wsl.box.model.boxentities.{BoxField, BoxForm}
import ch.wsl.box.model.shared._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{Auth, BoxConfig, UserProfile}
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import scribe.Logging

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Created by andreaminetti on 10/03/16.
  *
  * mapping from form specs in box schema into JSONForm
  */
object FormMetadataFactory{
  /**
   * Caches
   * cache keys contains lang identifier of the form (id or name) and username,
   * username it's crucial to avoid exposing rows that are not accessible by the user (in foreign keys)
   *
   * cache should be resetted when an external field changes
   */
  private val cacheFormName = scala.collection.mutable.Map[(String, String, String),JSONMetadata]()   //(up.name, form id, lang)
  private val cacheFormId = scala.collection.mutable.Map[(String, Int, String),JSONMetadata]()//(up.name, from name, lang)

  final val STATIC_PAGE = "box_static_page"

  def resetCache() = {
    cacheFormName.clear()
    cacheFormId.clear()
  }

  def resetCacheForEntity(e:String) = {
    cacheFormId.filter(c => CacheUtils.checkIfHasForeignKeys(e, c._2)).foreach{case (k,_) =>
      cacheFormId.remove(k)
    }
    cacheFormName.filter(c => CacheUtils.checkIfHasForeignKeys(e, c._2)).foreach{case (k,_) =>
      cacheFormName.remove(k)
    }
  }

  def hasGuestAccess(formName:String)(implicit ec:ExecutionContext, services: Services):DBIO[Option[UserProfile]] = {
    BoxFormTable.filter(f => f.name === formName && f.guest_user.nonEmpty).result.headOption
  }.map{_.map{ form =>
    new Auth().userProfileForUser(form.guest_user.get)
  }}


}



case class FormMetadataFactory()(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,services:Services) extends Logging with MetadataFactory {

  def list: DBIO[Seq[String]] = {
    BoxForm.BoxFormTable.result
  }.map{_.map(_.name)}

  def of(id:Int, lang:String):DBIO[JSONMetadata] = {
    val cacheKey = (up.name, id,lang)
    FormMetadataFactory.cacheFormId.get(cacheKey) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata cache miss! cache key: ($id,$lang), cache: ${FormMetadataFactory.cacheFormName}")
        val formQuery: Query[BoxForm.BoxForm, BoxForm_row, Seq] = for {
          form <- BoxForm.BoxFormTable if form.form_id === id
        } yield form

        for {
          metadata <- getForm(formQuery, lang)
        } yield {
          if(BoxConfig.enableCache) {
            FormMetadataFactory.cacheFormId.put(cacheKey,metadata)
            FormMetadataFactory.cacheFormName.put((up.name, metadata.name,lang),metadata)
          }
          metadata
        }

      }
    }
  }

  def of(name:String, lang:String):DBIO[JSONMetadata] = {
    val cacheKey = (up.name, name,lang)
    FormMetadataFactory.cacheFormName.lift(cacheKey) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata cache miss! cache key: ($name,$lang), cache: ${FormMetadataFactory.cacheFormName}")
        val formQuery: Query[BoxForm.BoxForm, BoxForm_row, Seq] = for {
          form <- BoxForm.BoxFormTable if form.name === name
        } yield form

        for {
          metadata <- getForm(formQuery, lang)
        } yield {
          if(BoxConfig.enableCache) {
            FormMetadataFactory.cacheFormName.put(cacheKey,metadata)
            FormMetadataFactory.cacheFormId.put((up.name, metadata.objId,lang),metadata)
          }
          metadata
        }


      }
    }

  }

  def children(form:JSONMetadata):DBIO[Seq[JSONMetadata]] = {
    val result = DBIO.sequence{
      form.fields.flatMap(_.child).map{ child =>
        of(child.objId,form.lang)
      }
    }
    val subresults = result.map(x => DBIO.sequence(x.map(children)))
    for{
      firstLevel <- result
      secondLevel <- subresults
      thirdLevel <- secondLevel.map(_.flatten)
    } yield {
      firstLevel ++ thirdLevel
    }

  }

  private def keys(form:BoxForm_row):DBIO[Seq[String]] = form.edit_key_field.map{x =>
    DBIO.successful(x.split(",").toSeq.map(_.trim))
  }.getOrElse(EntityMetadataFactory.keysOf(services.connection.dbSchema,form.entity))

  private def getForm(formQuery: Query[BoxForm.BoxForm,BoxForm_row,Seq], lang:String) = {

    import io.circe.generic.auto._

    def fieldQuery(formId:Int) = for{
      (field,fieldI18n) <- BoxField.BoxFieldTable joinLeft BoxField.BoxField_i18nTable.filter(_.lang === lang) on(_.field_id === _.field_id) if field.form_id === formId
    } yield (field,fieldI18n)

    val fQuery = formQuery joinLeft BoxForm.BoxForm_i18nTable.filter(_.lang === lang) on (_.form_id === _.form_id)


    val result = for{
      (form,formI18n) <- fQuery.result.map(_.head)
      fields <- fieldQuery(form.form_id.get).result
      fieldsFile <- DBIO.sequence(fields.map { case (f, _) =>
          BoxField.BoxFieldFileTable.filter(_.field_id === f.field_id).result.headOption
      })
      actions <- BoxForm.BoxForm_actions.filter(_.form_id === form.form_id.get).result

      cols <- new PgInformationSchema(services.connection.dbSchema,form.entity)(ec).columns
      columns = fields.map(f => cols.find(_.column_name == f._1.name))
      keys <- keys(form)
      jsonFieldsPartial <- fieldsToJsonFields(fields.zip(fieldsFile).zip(columns), lang)
    } yield {


      //to force adding primary keys if not specified by the user
      val missingKeyFields = keys.filterNot(k => fields.map(_._1.name).contains(k)).map{ key =>
        JSONField("string",key,false)
      }

      logger.info(s"Missing Key fields $missingKeyFields")

      if(formI18n.isEmpty) logger.warn(s"Form ${form.name} (form_id: ${form.form_id}) has no translation to $lang")

      val definedTableFields = form.tabularFields.toSeq.flatMap(_.split(",").map(_.trim))
      val missingKeyTableFields = keys.filterNot(k => definedTableFields.contains(k))
      val tableFields = missingKeyTableFields ++ definedTableFields

      val defaultQuery: Option[JSONQuery] = for{
        q <- form.query
        json <- parse(q).right.toOption
        jsonQuery <- json.as[JSONQuery].right.toOption
      } yield jsonQuery


      val jsonFields = {missingKeyFields ++ jsonFieldsPartial}.distinct

      def defaultLayout:Layout = { // for subform default with 12
        val default = Layout.fromFields(jsonFields)
        default.copy(blocks = default.blocks.map(_.copy(width = 12)))
      }

      val layout = Layout.fromString(form.layout).getOrElse(defaultLayout)

      val formActions = if(actions.isEmpty) {
        if(form.entity == FormMetadataFactory.STATIC_PAGE) {
          FormActionsMetadata.defaultForPages.copy(showNavigation = form.show_navigation)
        } else {
          FormActionsMetadata.default.copy(showNavigation = form.show_navigation)
        }
      } else {
        FormActionsMetadata(
          actions = actions.map{a =>
            FormAction(
              action = Action.fromString(a.action),
              importance = Importance.fromString(a.importance),
              afterActionGoTo = a.after_action_goto,
              label = a.label,
              updateOnly = a.update_only,
              insertOnly = a.insert_only,
              reload = a.reload,
              confirmText = a.confirm_text
            )
          },
          navigationActions = Seq(), // TODO
          showNavigation = form.show_navigation
        )
      }


      val keyStrategy = if(Managed(form.entity)) SurrugateKey else NaturalKey


      val result = JSONMetadata(
        form.form_id.get,
        form.name,
        formI18n.flatMap(_.label).getOrElse(form.name),
        jsonFields,
        layout,
        form.entity,
        lang,
        tableFields,
        form.tabularFields.toSeq.flatMap(_.split(",").map(_.trim)),
        keys,
        keyStrategy,
        defaultQuery,
        form.exportFields.map(_.split(",").map(_.trim).toSeq).getOrElse(tableFields),
        formI18n.flatMap(_.view_table),
        formActions,
        static = form.entity == FormMetadataFactory.STATIC_PAGE,
        dynamicLabel = formI18n.flatMap(_.dynamic_label)
      )//, form.entity)
      //println(s"resulting form: $result")
      result
    }

    result.transactionally

  }

  private def widget(field:BoxField_row,remoteEntity:String,remoteField:String) = field.params.flatMap(_.getOpt("widget")).getOrElse{
    val jsonType = Registry().fields.field(remoteEntity,remoteField).jsonType
    WidgetsNames.defaults.getOrElse(jsonType,WidgetsNames.input)
  }

  private def linkedForms(field:BoxField_row,field_i18n_row:Option[BoxField_i18n_row]):DBIO[Option[LinkedForm]] = {
    val linkedFormOpt = for{
      formId <- field.child_form_id
    } yield {
      for{
        lForm <- BoxForm.BoxFormTable.filter(_.form_id === formId).result
        keys <- keys(lForm(0))
      } yield {
        lForm.map{ value =>
          val parentFields = field.masterFields.toSeq.flatMap(_.split(",").map(_.trim))

          LinkedForm(
            value.name,
            parentFields,
            keys,
            lookup = field_i18n_row.flatMap(_.lookupTextField).map{ remoteField =>
              LookupLabel(
                localIds = parentFields,
                remoteIds = keys,
                remoteField = remoteField,
                remoteEntity = value.entity,
                widget = widget(field,value.entity,remoteField)
              )
            },
            label = field_i18n_row.flatMap(_.label)
          )

        }
      }
    }

    DBIO.sequence(linkedFormOpt.toSeq).map(_.flatten.headOption) // fix types
  }



  private def lookupLabel(field:BoxField_row,field_i18n_row: Option[BoxField_i18n_row]):Option[LookupLabel] = {
    for{
      localIds <- field.masterFields
      remoteIds <- field.lookupValueField
      remoteField <- field_i18n_row.flatMap(_.lookupTextField)
      remoteEntity <- field.lookupEntity
    } yield {



      LookupLabel(
        localIds = localIds.split(",").map(_.trim),
        remoteIds = remoteIds.split(",").map(_.trim),
        remoteField = remoteField,
        remoteEntity = remoteEntity,
        widget = widget(field,remoteEntity,remoteField)
      )
    }
  }

  private def condition(field:BoxField_row) = for{
    fieldId <- field.conditionFieldId
    values <- field.conditionValues
    json <- Try(parse(values).right.get.as[Seq[Json]].right.get).toOption
  } yield ConditionalField(fieldId,json)

  private def file(ff:BoxFieldFile_row) = FileReference(ff.name_field, ff.file_field, ff.thumbnail_field)

  private def label(field:BoxField_row,fieldI18n:Option[BoxField_i18n_row], lang:String):DBIO[String] = {

    field.child_form_id match {
      case None => DBIO.successful(fieldI18n.flatMap(_.label).getOrElse(field.name))
      case Some(subformId) => {
        {
          for{
            (form,formI18n) <- BoxForm.BoxFormTable joinLeft BoxForm_i18nTable.filter(_.lang === lang) on (_.form_id === _.form_id) if form.form_id === subformId
          } yield (formI18n,form)
        }.result.map{x => x.head._1.flatMap(_.label).getOrElse(x.head._2.name)}
      }
    }
  }

  private def subform(field:BoxField_row) = field.`type` match {
    case JSONFieldTypes.CHILD => {

      import io.circe.generic.auto._

      val childQuery: Option[JSONQuery] = for {
        filter <- field.childQuery
        json <- parse(filter).right.toOption
        result <- json.as[JSONQuery].right.toOption
      } yield result

      (field.childQuery, childQuery) match {
        case (Some(f), None) => logger.warn(s"$f not parsed correctly")
        case _ => {}
      }

      BoxForm.BoxFormTable.filter(_.form_id === field.child_form_id.getOrElse(0)).map(_.props).result.map { props =>

        for {
          id <- field.child_form_id
        } yield {
          val local = field.masterFields.getOrElse("")
          val remote = field.childFields.getOrElse("")
          Child(id, field.name, local, remote, childQuery, props.flatten.headOption.getOrElse(""))
        }
      }
    }
    case _ => DBIO.successful(None)
  }

  private def lookup(field:BoxField_row,fieldI18n:Option[BoxField_i18n_row], lang:String): DBIO[Option[JSONFieldLookup]] = {for{
    refEntity <- field.lookupEntity
    value <- field.lookupValueField

    text = fieldI18n.flatMap(_.lookupTextField).getOrElse(EntityMetadataFactory.lookupField(refEntity,lang,None))
  } yield {

    import io.circe.generic.auto._

    //implicit def fDb = FullDatabase(up.db,adminDb)

    for{
      keys <- EntityMetadataFactory.keysOf(services.connection.dbSchema,refEntity)
      filter = { for{
        queryString <- field.lookupQuery
        queryJson <- parse(queryString).right.toOption
        query <- queryJson.as[JSONQuery].right.toOption
      } yield query }.getOrElse(JSONQuery.sortByKeys(keys))

      lookupData <- DBIO.from(up.db.run(Registry().actions(refEntity).find(filter.copy(lang = Some(lang)))))

    } yield {
      Some(JSONFieldLookup.fromData(refEntity, JSONFieldMap(value,text,field.masterFields.getOrElse(field.name)), lookupData,field.lookupQuery))
    }

  }} match {
    case Some(a) => a
    case None => DBIO.successful(None)
  }

  private def fieldsToJsonFields(fields:Seq[(((BoxField_row,Option[BoxField_i18n_row]),Option[BoxFieldFile_row]),Option[PgColumn])], lang:String): DBIO[Seq[JSONField]] = {

    val jsonFields = fields.map{ case (((field,fieldI18n),fieldFile),pgColumn) =>

      if(fieldI18n.isEmpty) logger.warn(s"Field ${field.name} (field_id: ${field.field_id}) has no translation to $lang")

      for{
        look <- lookup(field, fieldI18n, lang)
        lab <- label(field, fieldI18n, lang)
        linked <- linkedForms(field, fieldI18n)
        subform <- subform(field)
      } yield {
        JSONField(
          `type` = field.`type`,
          name = field.name,
          nullable = !pgColumn.exists(_.required),
          readOnly = field.read_only.getOrElse(false),
          label = Some(lab),
          lookup = look,
          dynamicLabel = if(look.isEmpty) fieldI18n.flatMap(_.lookupTextField) else None,
          placeholder = fieldI18n.flatMap(_.placeholder),
          widget = field.widget,
          child = subform,
          default = field.default,
          file = fieldFile.map(file),
          condition = condition(field),
          tooltip = fieldI18n.flatMap(_.tooltip),
          params = field.params,
          linked = linked,
          lookupLabel = lookupLabel(field,fieldI18n),
          query = for{
            q <- field.childQuery.orElse(field.lookupQuery)
            js <- parse(q).toOption
            query <- js.as[JSONQuery].toOption
          } yield query
        )
      }

    }

    DBIO.sequence(jsonFields)

  }

}
