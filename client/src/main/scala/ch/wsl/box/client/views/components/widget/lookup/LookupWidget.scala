package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.views.components.widget.{HasData, Widget}
import ch.wsl.box.model.shared.{JSONField, JSONFieldLookup, JSONFieldTypes, JSONLookup, JSONMetadata, JSONQuery}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash.properties.Properties.ReadableSeqProperty
import io.udash.{Registration, SeqProperty, bind}
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
  val fieldLookup:JSONFieldLookup = field.lookup match {
    case Some(value) => value
    case None => throw new Exception(s"Lookupwidget on a non lookup field. Field ${field.name} form ${metadata.name}")
  }
  def metadata:JSONMetadata
  def lookup:ReadableSeqProperty[JSONLookup] = _lookup
//  {
////    _lookup.combine(data){case (l,d) =>
////      (current() ++ l).distinct
////    }
//
//  }


  private val _lookup:SeqProperty[JSONLookup] = {
    SeqProperty(Seq())
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


  private def setNewLookup(newLookup:Seq[JSONLookup],_data:Option[Json]) = {

    if (newLookup.exists(_.id != Json.Null) && newLookup.length != lookup.get.length || newLookup.exists(lu => lookup.get.exists(_.id != lu.id))) {
      _lookup.set(newLookup)
      _data.foreach{ d =>
        newLookup.find(_.id == d).foreach{ newModel =>
          model.set(Some(newModel))
        }
      }
      registerDataSync()
//      if(!newLookup.exists(_.id == data.get)) {
//        logger.info("Old value not exists")
//        newLookup.headOption.foreach{x =>
//          logger.info(s"Setting model to $x")
//          model.set(x,true)
//        }
//      }

    }
  }


  var dataSyncRegistration:Option[Registration] = None

  def fetchRemoteLookup(q:JSONQuery) = {
    dataSyncRegistration.foreach(_.cancel())
    logger.debug(s"Fetching remote lookup $q")

    val cacheKey = metadata.name + typings.jsMd5.mod.^(fieldLookup.lookupEntity + fieldLookup.map + q.toString)

    val currentData = data.get

    _lookup.set(Seq(), true) //reset lookup state


      LookupWidget.remoteLookup.get(cacheKey) match {
        case Some(value) => setNewLookup(value,Some(currentData))
        case _ => {

          logger.debug(s"Calling lookup with $q")

          services.rest.lookup( metadata.kind,services.clientSession.lang(), metadata.name, field.name, q, public).map { lookups =>
            logger.debug(s"Lookup $lookups fetched from ${fieldLookup.lookupEntity} for field ${field.name}")
            if (lookups.nonEmpty) {
              LookupWidget.remoteLookup.put(cacheKey, toSeq(lookups))
            }
            val newLookup = toSeq(lookups)
            setNewLookup(newLookup,Some(currentData))
          }
      }
    }
  }




  override def killWidget(): Unit = {
    LookupWidget.remoteLookup.filter(_._1.startsWith(metadata.name)).foreach(k => LookupWidget.remoteLookup.remove(k._1))
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


    if (fieldLookup.lookupQuery.isDefined &&  fieldLookup.lookupQuery.get.find(_ == '#').nonEmpty) {

      val query = fieldLookup.lookupQuery.get

      val variables = extractVariables(query)


      allData.listen({ allJs =>

          val q = variables.foldRight(query) { (variable, finalQuery) =>
            finalQuery.replaceAll("#" + variable, "\"" + allJs.get(variable) + "\"")
          }
          fetchRemoteLookup(JSONQuery.fromString(q).getOrElse(JSONQuery.empty.limit(1000)))

      }, true)
    } else {
      fetchRemoteLookup(fieldLookup.lookupQuery.flatMap(JSONQuery.fromString).getOrElse(JSONQuery.empty.limit(1000)))
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



}