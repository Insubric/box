package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.views.components.widget.{HasData, Widget}
import ch.wsl.box.model.shared.JSONQueryFilter.WHERE
import ch.wsl.box.model.shared.{JSONField, JSONFieldLookup, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONFieldTypes, JSONLookup, JSONMetadata, JSONQuery}
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash.bindings.modifiers.Binding
import io.udash.properties.Properties.ReadableSeqProperty
import io.udash.{Registration, SeqProperty, bind}
import io.udash.properties.single.{Property, ReadableProperty}
import scalatags.JsDom
import scalatags.JsDom.all.{label => lab, _}

import scala.concurrent.{ExecutionContext, Future}

object LookupWidget {
  var remoteLookup:scala.collection.mutable.Map[String,Future[Seq[JSONLookup]]] = scala.collection.mutable.Map()
}

trait LookupWidget extends Widget with HasData {

  import ch.wsl.box.client.Context._


  def allData:ReadableProperty[Json]
  def public:Boolean
  def field:JSONField


  val fieldLookup:JSONFieldLookup = field.lookup match {
    case Some(value) => value
    case None => throw new Exception(s"Lookupwidget on a non lookup field. Field ${field.name} form ${metadata.name}")
  }
  def metadata:JSONMetadata
  def lookup:ReadableSeqProperty[JSONLookup] = _lookup


  private val _lookup:SeqProperty[JSONLookup] = {
    SeqProperty(Seq())
  }

  val model:Property[Option[JSONLookup]] = Property(None)


  override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
    nested(bind(model.combine(data)((a,b) => (a,b)).transform{
      case (Some(notFound),js) if notFound.value == Labels.lookup.not_found => js.string
      case (t,d) => {
        t.map(_.value).getOrElse("")
      }
    }))
  }
  override def text() = model.transform(_.map(_.value).getOrElse(""))


  private def setNewLookup(_newLookup:Seq[JSONLookup]):Seq[JSONLookup] = {

    val newLookup = if(data.get != Json.Null && !_newLookup.exists(_.id == data.get) && data.get.string != JSONUtils.FIRST ) {
      _newLookup ++ Seq(JSONLookup(data.get,Seq(data.get.string)))
    } else _newLookup

    if (newLookup.exists(_.id != Json.Null) && newLookup.length != lookup.get.length || newLookup.exists(lu => lookup.get.exists(_.id != lu.id))) {


      _lookup.set(newLookup)
      val firstLookup = newLookup.find(_.id == data.get) // search the lookup associated with the current data

      if(field.default.contains(JSONUtils.FIRST) && firstLookup.isEmpty) {
        model.set(newLookup.headOption)
        newLookup.headOption.foreach(d => data.set(d.id))

      } else { // when everything ok
        model.set(firstLookup)
      }


      registerDataSync()
    }
    newLookup
  }


  var dataSyncRegistration:Option[Registration] = None

  override def killWidget(): Unit = {
    LookupWidget.remoteLookup.filter(_._1.startsWith(metadata.name)).foreach(k => LookupWidget.remoteLookup.remove(k._1))
    dataSyncRegistration.foreach(_.cancel())
    super.killWidget()
  }


  private def fetchRemoteLookup(fieldLookup:JSONFieldLookupRemote)(q: JSONQuery)(implicit ec: ExecutionContext):Future[Seq[JSONLookup]] = {
    dataSyncRegistration.foreach(_.cancel())
    logger.debug(s"Fetching remote lookup $q")

    val cacheKey = metadata.name + typings.jsMd5.mod.^(fieldLookup.lookupEntity + fieldLookup.map + q.toString)




    LookupWidget.remoteLookup.get(cacheKey) match {
      case Some(value) => value.map(setNewLookup)
      case _ => {
        _lookup.set(Seq(), true) //reset lookup state

        val request = for{
          lookups <- services.rest.lookup(metadata.kind, services.clientSession.lang(), metadata.name, field.name, q, public)
          singleLookup <- if(data.get != Json.Null && !lookups.exists(_.id == data.get)) {
            services.rest.lookup(metadata.kind, services.clientSession.lang(), metadata.name, field.name, JSONQuery.filterWith(WHERE.in(fieldLookup.map.valueProperty,Seq(data.get.string))), public)
          } else Future.successful(Seq[JSONLookup]())
        } yield {
          logger.debug(s"Lookup $lookups fetched from ${fieldLookup.lookupEntity} for field ${field.name}")
          if (lookups.isEmpty) {
            LookupWidget.remoteLookup.remove(cacheKey)
          }

          val allLookups = singleLookup ++ lookups
          setNewLookup(allLookups)
          allLookups
        }



        logger.debug(s"Calling lookup with $q")
        LookupWidget.remoteLookup.put(cacheKey, request)
        request

      }
    }
  }

  private def remoteLookup(fieldLookup:JSONFieldLookupRemote)(implicit ex:ExecutionContext) = {


    fieldLookup.lookupQuery.flatMap(JSONQuery.fromJson) match {
      case Some(query) => {
        autoRelease(allData.listen({ allJs =>
          val newQuery = query.withData(allJs,services.clientSession.lang())
          fetchRemoteLookup(fieldLookup)(newQuery)

        }, true))
      }
      case None => fetchRemoteLookup(fieldLookup)(JSONQuery.empty.limit(1000))
    }


  }



  def registerDataSync() = {
    dataSyncRegistration.foreach(_.cancel())
    dataSyncRegistration = Some(data.sync[Option[JSONLookup]](model)(
      { json: Json =>
        val result = lookup.get.find(_.id == json).getOrElse {
          if (!json.isNull)
            logger.warn(s"Lookup for $json not found on field ${field.name}")
          JSONLookup(json, Seq(json.string))
        }
        Some(result)
      },
      { jsonLookup: Option[JSONLookup] => jsonLookup.map(_.id).asJson }
    ))
  }

  override protected def loadWidget(): Unit = {
    super.loadWidget()

    fieldLookup match {
      case r: JSONFieldLookupRemote => {
        import ch.wsl.box.client.Context.Implicits._
        remoteLookup(r)
      }
      case JSONFieldLookupExtractor(extractor) => {
        autoRelease(allData.listen({ all =>
          logger.debug(s"Field ${field.name} extracting ${extractor.key} with ${extractor.map} from $all with data ${all.js(extractor.key)} and lookups ${extractor.map.get(all.js(extractor.key))}")
          extractor.map.get(all.js(extractor.key)) match {
            case Some(newLookup) => setNewLookup(newLookup)
            case None => {
              logger.warn(s"Extractor for ${field.name} on ${extractor.key} with data ${all.js(extractor.key)} failed")
              setNewLookup(Seq())
            }
          }
        }, true))
      }
      case JSONFieldLookupData(data) => setNewLookup(data)
    }
  }

  private def extractUserData(data:Seq[JSONLookup])(json:Json) = data.find(_.id == json).map(x => Json.fromString(x.value)).getOrElse(Json.Null)
  private def extractDataFromLabel(data:Seq[JSONLookup])(label:String) = data.find(_.value == label).map(x => x.id).getOrElse(Json.Null)


  private def fetchLookups()(implicit ec: ExecutionContext):Future[Seq[JSONLookup]] = {
    fieldLookup match {
      case r:JSONFieldLookupRemote => {
        r.lookupQuery.flatMap(JSONQuery.fromJson) match {
          case Some(query) => {
            val newQuery = query.withData(allData.get,services.clientSession.lang())
            fetchRemoteLookup(r)(newQuery)(ec)

          }
          case None => fetchRemoteLookup(r)(JSONQuery.empty.limit(1000))(ec)
        }
      }
      case JSONFieldLookupExtractor(extractor) => Future.successful(
        extractor.map.get(allData.get.js(extractor.key)).toList.flatten
      )
      case JSONFieldLookupData(data) => Future.successful(data)
    }
  }

  override def valid()(implicit ec: ExecutionContext): Future[Boolean] = {
    fetchLookups().map(_.exists(_.id == data.get))
  }

  override def fromLabel(str: String)(implicit ec: ExecutionContext): Future[Json] = {
    fetchLookups().map(d => extractDataFromLabel(d)(str))
  }

  override def toUserReadableData(json: Json)(implicit ex:ExecutionContext): Future[Json] = {
    fetchLookups().map(d => extractUserData(d)(json))
  }
}