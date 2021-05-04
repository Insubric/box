package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.Labels
import ch.wsl.box.client.views.components.widget.{HasData, Widget}
import ch.wsl.box.model.shared.{JSONField, JSONLookup}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash.{SeqProperty, bind}
import io.udash.properties.single.{Property, ReadableProperty}
import scalatags.JsDom

object LookupWidget {
  var remoteLookup:scala.collection.mutable.Map[String,Seq[JSONLookup]] = scala.collection.mutable.Map()
}

trait LookupWidget extends Widget with HasData {

  import ch.wsl.box.client.Context._


  def allData:ReadableProperty[Json]


  def field:JSONField
  val lookup:SeqProperty[JSONLookup] = {
    SeqProperty(toSeq(field.lookup.toSeq.flatMap(_.lookup)))
  }

  val model:Property[JSONLookup] = Property(JSONLookup("",""))

  field.lookup.get.lookupExtractor.foreach{case extractor =>
    allData.listen({ all =>
      val newLookup = toSeq(extractor.map.getOrElse(all.js(extractor.key), Seq()))
      setNewLookup(newLookup)
    },true)
  }

  field.`type` match {
    case "number" =>  data.sync[JSONLookup](model)(
      {json:Json =>
        val id = jsonToString(json)
        lookup.get.find(_.id == jsonToString(json)).getOrElse(JSONLookup(id,id + " NOT FOUND"))
      },
      {jsonLookup:JSONLookup => strToNumericJson(jsonLookup.id)}
    )
    case _ => data.sync[JSONLookup](model)(
      {json:Json =>
        val id = jsonToString(json)
        val result = lookup.get.find(_.id == id).getOrElse(JSONLookup(id,id + " NOT FOUND"))
        result
      },
      {jsonLookup:JSONLookup => strToJson(field.nullable)(jsonLookup.id)}
    )
  }


  val selectModel = data.transform(value2Label)


  override def showOnTable(): JsDom.all.Modifier = {
    autoRelease(bind(selectModel.combine(data)((a,b) => (a,b)).transform{
      case (notFound,js) if notFound == Labels.lookup.not_found => js.string
      case (t,_) => t
    }))
  }
  override def text() = selectModel




  private def toSeq(s:Seq[JSONLookup]):Seq[JSONLookup] = if(field.nullable) {
    Seq(JSONLookup("","")) ++ s
  } else {
    s
  }

  private def setNewLookup(newLookup:Seq[JSONLookup]) = {
    if (newLookup.exists(_.id.nonEmpty) && newLookup.length != lookup.get.length || newLookup.exists(lu => lookup.get.exists(_.id != lu.id))) {
      lookup.set(newLookup, true)
      if(!newLookup.exists(_.id == data.get.string)) {
        logger.info("Old value not exists")
        newLookup.headOption.foreach{x =>
          logger.info(s"Setting model to $x")
          model.set(x,true)
        }
      }

    }
  }


  var widgetCacheKeys:scala.collection.mutable.Set[String] = scala.collection.mutable.Set()

  for{
    look <- field.lookup
    query <- look.lookupQuery
  } yield {
    if(query.find(_ == '#').nonEmpty) {

      val variables =extractVariables(query)

      val queryWithSubstitutions = Property("")

      autoRelease(allData.listen({ json =>
        val q = variables.foldRight(query){(variable, finalQuery) =>
          finalQuery.replaceAll("#" + variable, "\"" + json.js(variable).string + "\"")
        }
        if(queryWithSubstitutions.get != q)
          queryWithSubstitutions.set(q)
      },true))

      autoRelease(queryWithSubstitutions.listen({ q =>

        val cacheKey =  typings.jsMd5.mod.^(look.lookupEntity + look.map + q)
        lookup.set(Seq(), true) //reset lookup state

        widgetCacheKeys.add(q)

        if(q.nonEmpty) {
          LookupWidget.remoteLookup.get(cacheKey) match {
            case Some(value) => setNewLookup(value)
            case None => {
              val jsonQuery = parser.parse(q) match {
                case Left(e) => {
                  logger.error(e.message)
                  Json.Null
                }
                case Right(j) => j
              }

              services.rest.lookup(services.clientSession.lang(), look.lookupEntity, look.map, jsonQuery).map { lookups =>
                if(lookups.nonEmpty) {
                  LookupWidget.remoteLookup.put(cacheKey, toSeq(lookups))
                }
                setNewLookup(toSeq(lookups))
              }
            }
          }



        }

      }, true))
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



  private def value2Label(org:Json):String = {

    val lookupValue = allData.get.get(field.lookup.get.map.localValueProperty)

    lookup.get.find(_.id == lookupValue).map(_.value)
      .orElse(field.lookup.get.lookup.find(_.id == org.string).map(_.value))
      .getOrElse(Labels.lookup.not_found)
  }
}