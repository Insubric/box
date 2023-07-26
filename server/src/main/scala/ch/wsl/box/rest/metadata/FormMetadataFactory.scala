package ch.wsl.box.rest.metadata

import java.util.UUID
import akka.stream.Materializer
import ch.wsl.box.information_schema.{PgColumn, PgColumns, PgInformationSchema}
import ch.wsl.box.jdbc.{Connection, FullDatabase, Managed, UserDatabase}
import ch.wsl.box.model.boxentities.BoxField.{BoxField_i18n_row, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_i18nTable, BoxForm_row}
import ch.wsl.box.model.boxentities.{BoxField, BoxForm}
import ch.wsl.box.model.shared._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.runtime.{ColType, Registry}
import ch.wsl.box.rest.utils.{Auth, BoxSession, UserProfile}
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
object FormMetadataFactory extends Logging with MetadataFactory{
  /**
   * Caches
   * cache keys contains lang identifier of the form (id or name) and username,
   * username it's crucial to avoid exposing rows that are not accessible by the user (in foreign keys)
   *
   * cache should be resetted when an external field changes
   */
  private val cacheFormName = scala.collection.mutable.Map[(String, String),JSONMetadata]()   //(up.name, form id, lang)
  private val cacheFormId = scala.collection.mutable.Map[(UUID, String),JSONMetadata]()//(up.name, from name, lang)

  final val STATIC_PAGE = "box_static_page"

  def resetCache() = {
    cacheFormName.clear()
    cacheFormId.clear()
  }


  def hasGuestAccess(formName:String)(implicit ec:ExecutionContext, services: Services):Future[Option[BoxSession]] = {

    for{
      form <- services.connection.adminDB.run(BoxFormTable.filter(f => f.name === formName && f.guest_user.nonEmpty).result.headOption)
      user = form.flatMap(_.guest_user)
      roles <-  user match {
        case Some(u) => Auth.rolesOf(u)
        case None => Future.successful(Seq())
      }
    } yield user.map(u => BoxSession(CurrentUser(u,roles)))
  }


  def list(implicit ec:ExecutionContext,services:Services): DBIO[Seq[String]] = {
    BoxForm.BoxFormTable.result
  }.map{_.map(_.name)}

  def filterRoles(user:CurrentUser)(metadata:DBIO[JSONMetadata])(implicit ec:ExecutionContext):DBIO[JSONMetadata] = metadata.map{ m =>

    def filterRole(field:JSONField):Boolean = field.roles.isEmpty || field.roles.intersect(user.roles ++ Seq(user.username)).nonEmpty

    m.copy(fields = m.fields.filter(filterRole))
  }

  def of(id:UUID, lang:String, user:CurrentUser)(implicit ec:ExecutionContext,services:Services):DBIO[JSONMetadata] = filterRoles(user){
    val cacheKey = (id,lang)
    FormMetadataFactory.cacheFormId.get(cacheKey) match {
      case Some(r) => DBIO.successful(r)
      case None => {
        logger.info(s"Metadata cache miss! cache key: ($id,$lang), cache: ${FormMetadataFactory.cacheFormName}")
        val formQuery: Query[BoxForm.BoxForm, BoxForm_row, Seq] = for {
          form <- BoxForm.BoxFormTable if form.form_uuid === id
        } yield form

        for {
          metadata <- getForm(formQuery, lang)
        } yield {
          if(services.config.enableCache) {
            FormMetadataFactory.cacheFormId.put(cacheKey,metadata)
            FormMetadataFactory.cacheFormName.put((metadata.name,lang),metadata)
          }
          metadata
        }

      }
    }
  }

  def of(name:String, lang:String, user:CurrentUser)(implicit ec:ExecutionContext,services:Services):DBIO[JSONMetadata] = filterRoles(user){
    val cacheKey = (name,lang)
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
          if(services.config.enableCache) {
            FormMetadataFactory.cacheFormName.put(cacheKey,metadata)
            FormMetadataFactory.cacheFormId.put((metadata.objId,lang),metadata)
          }
          metadata
        }


      }
    }

  }

  def children(form:JSONMetadata,currentUser: CurrentUser,ignoreChilds:Seq[UUID] = Seq())(implicit ec:ExecutionContext,services:Services):DBIO[Seq[JSONMetadata]] = {
    val childIds: Seq[UUID] = form.fields.flatMap(_.child).map(_.objId)

    for{
      firstLevel <- DBIO.sequence{childIds.map{ objId => of(objId,form.lang,currentUser)}}
      secondLevel <- DBIO.sequence(firstLevel.map(y => children(y,currentUser,ignoreChilds ++ childIds)))
    } yield {
      firstLevel ++ secondLevel.flatten
    }

  }

  private def keys(form:BoxForm_row)(implicit ec:ExecutionContext,services:Services):DBIO[Seq[String]] = form.edit_key_field.map{x =>
    DBIO.successful(x.split(",").toSeq.map(_.trim))
  }.getOrElse(EntityMetadataFactory.keysOf(services.connection.dbSchema,form.entity))

  private def toConditions(json:Json):Seq[ConditionalField] = {
    json.as[Map[String, Json]] match {
      case Left(value) => {
        logger.warn(s"Failed to decode condition: ${value.getMessage()} on $json")
        Seq()
      }
      case Right(value) => value.map{ case (k,v) => ConditionalField(k,v)}.toSeq
    }
  }

  private def getForm(formQuery: Query[BoxForm.BoxForm,BoxForm_row,Seq], lang:String)(implicit ec:ExecutionContext,services:Services) = {

    import io.circe.generic.auto._

    def fieldQuery(formId:UUID) = for{
      (field,fieldI18n) <- BoxField.BoxFieldTable joinLeft BoxField.BoxField_i18nTable.filter(_.lang === lang) on(_.field_uuid === _.field_uuid) if field.form_uuid === formId
    } yield (field,fieldI18n)

    val fQuery = formQuery joinLeft BoxForm.BoxForm_i18nTable.filter(_.lang === lang) on (_.form_uuid === _.form_uuid)


    val result = for{
      (form,formI18n) <- fQuery.result.map(_.head)
      fields <- fieldQuery(form.form_uuid.get).result
      actions <- BoxForm.BoxForm_actions.filter(_.form_uuid === form.form_uuid.get).sortBy(_.action_order).result
      navigationActions <- BoxForm.BoxForm_navigation_actions.filter(_.form_uuid === form.form_uuid.get).sortBy(_.action_order).result
      tableActions <- BoxForm.BoxForm_table_actions.filter(_.form_uuid === form.form_uuid.get).sortBy(_.action_order).result
      topTableActions <- BoxForm.BoxForm_top_table_actions.filter(_.form_uuid === form.form_uuid.get).sortBy(_.action_order).result
      columns = fields.map(f => EntityMetadataFactory.fieldType(form.entity,f._1.name,Registry()).getOrElse(ColType.unknown))
      keys <- keys(form)
      jsonFieldsPartial <- fieldsToJsonFields(fields.zip(columns), lang)
    } yield {


      //to force adding primary keys if not specified by the user
      val missingKeyFields = keys.filterNot(k => fields.map(_._1.name).contains(k)).map{ key =>
        JSONField("string",key,false)
      }

      logger.info(s"Missing Key fields $missingKeyFields")

      if(formI18n.isEmpty) logger.warn(s"Form ${form.name} (form_id: ${form.form_uuid}) has no translation to $lang")

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
              confirmText = a.confirm_text,
              executeFunction = a.execute_function,
              condition = a.condition.map(toConditions),
              html5check = a.html_check,
              target = a.target.map(Target.fromString).getOrElse(Self)
            )
          },
          navigationActions = navigationActions.map{a =>
            FormAction(
              action = Action.fromString(a.action),
              importance = Importance.fromString(a.importance),
              afterActionGoTo = a.after_action_goto,
              label = a.label,
              updateOnly = a.update_only,
              insertOnly = a.insert_only,
              reload = a.reload,
              confirmText = a.confirm_text,
              executeFunction = a.execute_function
            )
          },
          tableActions = if(tableActions.isEmpty && keys.nonEmpty) FormActionsMetadata.default.tableActions else {
            tableActions.map { a =>
              FormAction(
                action = Action.fromString(a.action),
                importance = Importance.fromString(a.importance),
                afterActionGoTo = a.after_action_goto,
                label = a.label,
                updateOnly = a.update_only,
                insertOnly = a.insert_only,
                reload = a.reload,
                confirmText = a.confirm_text,
                executeFunction = a.execute_function,
                needDeleteRight = a.need_delete_right,
                needUpdateRight = a.need_update_right,
                whenNoUpdateRight = a.when_no_update_right,
                target = a.target.map(Target.fromString).getOrElse(Self)
              )
          }},
          topTableActions = if(topTableActions.isEmpty && keys.nonEmpty) FormActionsMetadata.default.topTableActions else {
            topTableActions.map { a =>
              FormAction(
                action = Action.fromString(a.action),
                importance = Importance.fromString(a.importance),
                afterActionGoTo = a.after_action_goto,
                label = a.label,
                confirmText = a.confirm_text,
                executeFunction = a.execute_function,
                needDeleteRight = a.need_delete_right,
                needUpdateRight = a.need_update_right,
                needInsertRight = a.need_insert_right,
                whenNoUpdateRight = a.when_no_update_right,
                target = a.target.map(Target.fromString).getOrElse(Self)
              )
            }
          },
          showNavigation = form.show_navigation
        )
      }


      val keyStrategy = if(Managed(form.entity)) SurrugateKey else NaturalKey


      val result = JSONMetadata(
        form.form_uuid.get,
        form.name,
        EntityKind.FORM.kind,
        formI18n.flatMap(_.label).getOrElse(form.name),
        jsonFields,
        layout,
        form.entity,
        lang,
        tableFields,
        definedTableFields,
        keys,
        keyStrategy,
        defaultQuery,
        form.exportFields.map(_.split(",").map(_.trim).toSeq).getOrElse(tableFields),
        formI18n.flatMap(_.view_table),
        formActions,
        static = form.entity == FormMetadataFactory.STATIC_PAGE,
        dynamicLabel = formI18n.flatMap(_.dynamic_label),
        params = form.params
      )//, form.entity)
      //println(s"resulting form: $result")
      result
    }

    result.transactionally

  }

  private def widget(field:BoxField_row,remoteEntity:String,remoteField:String) = field.params.flatMap(_.getOpt("widget")).getOrElse{
    val jsonType = Registry().fields.field(remoteEntity,remoteField).getOrElse(ColType.unknown).jsonType
    WidgetsNames.defaults.getOrElse(jsonType,WidgetsNames.input)
  }

  private def linkedForms(field:BoxField_row,field_i18n_row:Option[BoxField_i18n_row])(implicit ec:ExecutionContext,services:Services):DBIO[Option[LinkedForm]] = {
    val linkedFormOpt = for{
      formId <- field.child_form_uuid
    } yield {
      for{
        lForm <- BoxForm.BoxFormTable.filter(_.form_uuid === formId).result
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
    json <- Try(parse(values).right.get.as[Json].right.get).toOption
  } yield ConditionalField(fieldId,json)


  private def label(field:BoxField_row,fieldI18n:Option[BoxField_i18n_row], lang:String)(implicit ec:ExecutionContext,services:Services):DBIO[String] = {

    field.child_form_uuid match {
      case None => DBIO.successful(fieldI18n.flatMap(_.label).getOrElse(field.name))
      case Some(subformId) => {
        {
          for{
            (form,formI18n) <- BoxForm.BoxFormTable joinLeft BoxForm_i18nTable.filter(_.lang === lang) on (_.form_uuid === _.form_uuid) if form.form_uuid === subformId
          } yield (formI18n,form)
        }.result.map{x => fieldI18n.flatMap(_.label).orElse(x.head._1.flatMap(_.label)).getOrElse(x.head._2.name)}
      }
    }
  }

  private def subform(field:BoxField_row)(implicit ec:ExecutionContext) = field.`type` match {
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

      BoxForm.BoxFormTable.filter(_.form_uuid === field.child_form_uuid.getOrElse(UUID.randomUUID())).map(_.props).result.map { props =>

        for {
          id <- field.child_form_uuid
          local <- field.masterFields
          remote <- field.childFields
        } yield {
          Child(id, field.name, local, remote, childQuery, props.flatten.headOption.getOrElse(""),field.widget.exists(WidgetsNames.childsWithData))
        }
      }
    }
    case _ => DBIO.successful(None)
  }

  private def lookup(field:BoxField_row,fieldI18n:Option[BoxField_i18n_row])(implicit ec:ExecutionContext,services:Services): Option[JSONFieldLookup] = {for{
    refEntity <- field.lookupEntity
    value <- field.lookupValueField
    text = fieldI18n.flatMap(_.lookupTextField).getOrElse(EntityMetadataFactory.lookupField(refEntity,None))
  } yield {

      Some(JSONFieldLookup.fromDB(refEntity, JSONFieldMap(value,text,field.masterFields.getOrElse(field.name)), field.lookupQuery))

  }} match {
    case Some(a) => a
    case None => None
  }

  private def fieldsToJsonFields(fields:Seq[((BoxField_row,Option[BoxField_i18n_row]),ColType)], lang:String)(implicit ec:ExecutionContext,services:Services): DBIO[Seq[JSONField]] = {

    val jsonFields = fields.map{ case ((field,fieldI18n),colType) =>

      if(fieldI18n.isEmpty) logger.warn(s"Field ${field.name} (field_id: ${field.field_uuid}) has no translation to $lang")

      for{
        lab <- label(field, fieldI18n, lang)
        look = lookup(field, fieldI18n)
        linked <- linkedForms(field, fieldI18n)
        subform <- subform(field)
      } yield {
        JSONField(
          `type` = field.`type`,
          name = field.name,
          nullable = colType.nullable && !field.required.getOrElse(false),
          readOnly = field.read_only,
          label = Some(lab),
          lookup = look,
          dynamicLabel = if(look.isEmpty) fieldI18n.flatMap(_.lookupTextField) else None,
          placeholder = fieldI18n.flatMap(_.placeholder),
          widget = field.widget,
          child = subform,
          default = field.default,
          condition = condition(field),
          tooltip = fieldI18n.flatMap(_.tooltip),
          params = field.params,
          linked = linked,
          lookupLabel = lookupLabel(field,fieldI18n),
          query = for{
            q <- field.childQuery.orElse(field.lookupQuery)
            js <- parse(q).toOption
            query <- js.as[JSONQuery].toOption
          } yield query,
          function = field.function,
          minMax = Some(MinMax(min = field.min, max = field.max)),
          roles = field.roles.getOrElse(Seq())
        )
      }

    }

    DBIO.sequence(jsonFields)

  }

}
