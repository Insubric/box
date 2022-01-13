package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.Labels
import ch.wsl.box.client.views.components.widget.{HasData, Widget}
import ch.wsl.box.model.shared.{JSONField, JSONFieldLookup, JSONFieldTypes, JSONLookup, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash.properties.seq.ReadableSeqProperty
import io.udash.{SeqProperty, bind}
import io.udash.properties.single.{Property, ReadableProperty}
import scalatags.JsDom

import scala.scalajs.js.timers.setTimeout

object LookupWidget {
  var remoteLookup:scala.collection.mutable.Map[String,Seq[JSONLookup]] = scala.collection.mutable.Map()
}

trait LookupWidget extends Widget with HasData {

  import ch.wsl.box.client.Context._


  def allData:ReadableProperty[Json]
  def public:Boolean


  def field:JSONField
  def metadata:JSONMetadata
  def lookup:ReadableProperty[Seq[JSONLookup]] = _lookup.combine(data){case (l,d) =>
    current() ++ l
  }

  private val _lookup:Property[Seq[JSONLookup]] = {
    Property(toSeq(field.lookup.toSeq.flatMap(_.lookup)))
  }

  val model:Property[Option[JSONLookup]] = Property(None)


  override def showOnTable(): JsDom.all.Modifier = {
    autoRelease(bind(model.combine(data)((a,b) => (a,b)).transform{
      case (Some(notFound),js) if notFound.value == Labels.lookup.not_found => js.string
      case (t,d) => {
        t.map(_.value).getOrElse("")
      }
    }))
  }
  override def text() = model.transform(_.map(_.value).getOrElse(""))

  private def toSeq(s:Seq[JSONLookup]):Seq[JSONLookup] = s

  private def current():Seq[JSONLookup] = {
    val current = field.lookup.toSeq.flatMap(_.allLookup.filter(x => data.get == x.id))
    logger.warn(s"Current $current")
    current
  }

  private def setNewLookup(_newLookup:Seq[JSONLookup],_data:Option[Json]) = {

    val newLookup:Seq[JSONLookup] = current() ++ _newLookup
    if (newLookup.exists(_.id != Json.Null) && newLookup.length != lookup.get.length || newLookup.exists(lu => lookup.get.exists(_.id != lu.id))) {
      _lookup.set(newLookup, true)
      _data.foreach{ d =>
        newLookup.find(_.id == d).foreach{ newModel =>
          model.set(Some(newModel))
        }
      }
//      if(!newLookup.exists(_.id == data.get)) {
//        logger.info("Old value not exists")
//        newLookup.headOption.foreach{x =>
//          logger.info(s"Setting model to $x")
//          model.set(x,true)
//        }
//      }

    }
  }


  var widgetCacheKeys:scala.collection.mutable.Set[String] = scala.collection.mutable.Set()

  def fetchRemoteLookup(q:String,look:JSONFieldLookup) = {

    logger.debug(s"Fetching remote lookup $q")

    val cacheKey = typings.jsMd5.mod.^(look.lookupEntity + look.map + q)

    val currentModel = model.get
    val currentData = data.get

    _lookup.set(Seq(), true) //reset lookup state

    widgetCacheKeys.add(q)

    if (q.nonEmpty) {
      LookupWidget.remoteLookup.get(cacheKey) match {
        case Some(value) => setNewLookup(value,Some(currentData))
        case _ => {
          val jsonQuery = parser.parse(q) match {
            case Left(e) => {
              logger.error(e.message)
              Json.Null
            }
            case Right(j) => j
          }

          logger.debug(s"Calling lookup with $jsonQuery")

          services.rest.lookup( metadata.kind,services.clientSession.lang(), metadata.name, field.name, jsonQuery, public).map { lookups =>
            logger.debug(s"Lookup $lookups fetched")
            if (lookups.nonEmpty) {
              LookupWidget.remoteLookup.put(cacheKey, toSeq(lookups))
            }
            val newLookup = toSeq(lookups)
            setNewLookup(newLookup,Some(currentData))
          }
        }
      }
    }
  }




  override def killWidget(): Unit = {
    widgetCacheKeys.foreach(k => LookupWidget.remoteLookup.remove(k))
    widgetCacheKeys.clear()
    super.killWidget()
  }

  private def extractVariables(query:String):Seq[String] = {
    query.zipWithIndex.filter(_._1 == '#').map{ case (_,i) =>
      val nextIndex = Seq(query.length,query.indexOf(' ',i),query.indexOf('}',i),query.indexOf(',',i)).min
      query.substring(i+1,nextIndex).replaceAll("\n","").trim
    }.distinct
  }

  field.lookup.get.lookupExtractor.foreach{case extractor =>
    allData.listen({ all =>
      val newLookup = toSeq(extractor.map.getOrElse(all.js(extractor.key), Seq()))
      setNewLookup(newLookup,None)
    },true)
  }

  for {
    look <- field.lookup
    query <- look.lookupQuery
  } yield {
    if (query.find(_ == '#').nonEmpty) {

      val variables = extractVariables(query)

      val variableData = allData.transform { js =>
        variables.map(v => (v, js.js(v))).toMap
      }
      logger.debug(s"Variables: ${variableData.get}")

      variableData.listen({ vars =>
        if(vars.values.forall(!_.isNull)) {
          val q = variables.foldRight(query) { (variable, finalQuery) =>
            finalQuery.replaceAll("#" + variable, "\"" + vars(variable).string + "\"")
          }
          fetchRemoteLookup(q,look)
        } else {
          _lookup.set(Seq())
        }
      }, true)
    }
  }

  data.sync[Option[JSONLookup]](model)(
    {json:Json =>
      val result = (lookup.get ++ current()).find(_.id == json).getOrElse{
        logger.warn(s"Lookup for $json not found")
        JSONLookup(json,Seq(json.string))
      }
      Some(result)
    },
    {jsonLookup:Option[JSONLookup] => jsonLookup.map(_.id).asJson}
  )

}