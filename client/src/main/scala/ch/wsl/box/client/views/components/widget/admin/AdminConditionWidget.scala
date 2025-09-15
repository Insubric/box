package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{AndCondition, Condition, ConditionFieldRef, ConditionValue, ConditionalField, EmptyCondition, EntityKind, Filter, JSONField, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter, JSONQueryPaging, JSONSort, NotCondition, OrCondition, Sort, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.utils.{BootstrapTags, UdashIcons}
import io.udash.css.CssView._
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import scalacss.ScalatagsCss._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}


object AdminConditionWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminConditionBuilder


  override def create(params: WidgetParams): Widget = AdminConditionWidgetImpl(params)

  case class AdminConditionWidgetImpl(params: WidgetParams) extends Widget with Logging {

    override def field: JSONField = params.field


    /**
     * Used to provide the user the human-readable representation of the data, mainly used for lookups
     *
     * @param json data as stored in the database
     * @param ex
     * @return user readable data
     */
    override def toUserReadableData(json: Json)(implicit ex: ExecutionContext): Future[Json] = Future.successful(json.as[Condition].map(_.asString).getOrElse("").asJson)

    override protected def show(nested: Binding.NestedInterceptor): JsDom.all.Modifier = edit(nested)

    import Condition._


    val columns:SeqProperty[String] = SeqProperty(Seq())

    var registration:Option[Registration] = None

    override def killWidget(): Unit = {
      super.killWidget()
      registration.foreach(_.cancel())
    }

    import ch.wsl.box.client.Context._
    import Implicits.executionContext

    def fetchFieldForEntity(entity:String):Unit  = {
      services.rest.metadata(EntityKind.ENTITY.kind,services.clientSession.lang(),entity,false).foreach { m =>
        columns.set(m.fields.map(_.name))
      }
    }

    params.fieldParams.foreach{x =>
      x.listen({ p =>
        p.getOpt("entity").foreach(fetchFieldForEntity)

        p.jsOpt("form").foreach{ f =>
          services.rest.get(EntityKind.BOX_FORM.kind,services.clientSession.lang(),"form",JSONID.fromMap(Seq(("form_uuid",f)))).foreach { m =>
            m.getOpt("entity").map(fetchFieldForEntity)
          }
        }

      },true)

    }

    def removeCondition(data:Property[Json]) = {
      button("Remove", ClientConf.style.boxButtonDanger, onclick := {(e:Event) =>
        data.set(EmptyCondition.asJson)
      })
    }


    def renderConditionValue(data:Property[Json]) = {
      val prop = data.bitransform{x =>
        x.as[ConditionValue].map(_.value.string).getOrElse("")
      }{  str =>
        io.circe.parser.parse(str).map(ConditionValue(_)).getOrElse(ConditionValue(Json.fromString(str))).asJson
      }
      div(
        ClientConf.style.adminConditionBlock,
        TextInput(prop,1 second)(placeholder := "value"),
        removeCondition(data)
      )
    }

    def renderConditionFieldRef(data:Property[Json]) = {
      val prop = data.bitransform{x =>
        x.as[ConditionFieldRef].map(_.valueField).getOrElse("")
      }{  str =>
        ConditionFieldRef(str).asJson
      }
      div(
        ClientConf.style.adminConditionBlock,
        "Value from field: ",
        Select(prop,columns)(x => x),
        removeCondition(data)

      )
    }

    def renderNot(data:Property[Json]) = {
      val prop = data.bitransform{x =>
        x.as[NotCondition].map(_.not.asJson).getOrElse(EmptyCondition.asJson)
      }{  condition =>
        condition.as[Condition].map(NotCondition(_).asJson).getOrElse(EmptyCondition.asJson)
      }
      div(
        ClientConf.style.adminConditionBlock,
        "Not:",
        renderCondition(prop),
        removeCondition(data)
      )

    }

    def addButton(prop:SeqProperty[Json]) = button("Add",ClientConf.style.boxButton, onclick :+= {(e:Event) =>
      prop.append(EmptyCondition.asJson)
    })

    def removeLine(prop:SeqProperty[Json],i:ReadableProperty[Int]) = button("Remove line",ClientConf.style.boxButton, onclick :+= {(e:Event) =>
      prop.remove(i.get,1)
    })


    def renderRepetable(label:String, data:Property[Json],prop:SeqProperty[Json]) = {
      div(
        ClientConf.style.adminConditionBlock,
        s"$label:",
        ul(
          flexGrow := 1,
          repeatWithIndex(prop){ (p,i,n) =>
            li(
              renderCondition(p),
              removeLine(prop,i)
            ).render
          },
          li(addButton(prop),removeCondition(data))
        ),

      )
    }

    def renderOr(data:Property[Json]) = {

      val prop:SeqProperty[Json] = data.bitransformToSeq {x =>
        x.as[OrCondition].map(_.or.map(_.asJson)).getOrElse(Seq())
      }{  conditions =>
        OrCondition(conditions.toSeq.flatMap(_.as[Condition].toOption)).asJson
      }

      renderRepetable("OR",data,prop)

    }

    def renderAnd(data:Property[Json]) = {
      val prop:SeqProperty[Json] = data.bitransformToSeq {x =>
        x.as[AndCondition].map(_.and.map(_.asJson)).getOrElse(Seq())
      }{  conditions =>
        AndCondition(conditions.toSeq.flatMap(_.as[Condition].toOption)).asJson
      }


      renderRepetable("AND",data,prop)
    }

    def renderConditionalField(data:Property[Json]) = {
      val propCondition:Property[Json] = data.bitransform {x =>
        x.as[ConditionalField].map(_.condition.asJson).getOrElse(EmptyCondition.asJson)
      }{  condition =>
        val fc = for {
          orig <- data.get.as[ConditionalField]
          cond <- condition.as[Condition]
        } yield orig.copy(condition = cond)
        fc.toOption.asJson
      }

      val propField:Property[String] = data.bitransform {x =>
        x.as[ConditionalField].map(_.field).getOrElse("")
      }{  str =>
        val fc = for {
          orig <- data.get.as[ConditionalField]
        } yield orig.copy(field = str)
        fc.toOption.asJson
      }

      div(
        ClientConf.style.adminConditionBlock,
        Select(propField,columns)(x => x),
        renderCondition(propCondition),
        removeCondition(data)
      )
    }


    def emptyButtons(data:Property[Json], firstLevel:Boolean) = div(
      button("Field",ClientConf.style.boxButton, onclick :+= { (e:Event) =>
        val newData = ConditionalField(columns.get.head,EmptyCondition).asJson
        data.set(newData,true)
      }),
      button("OR",ClientConf.style.boxButton, onclick :+= ((e:Event) => data.set(OrCondition(Seq()).asJson,true))),
      button("AND",ClientConf.style.boxButton,  onclick :+= ((e:Event) => data.set(AndCondition(Seq()).asJson,true))),
      if(!firstLevel) Seq(
        button("Value",ClientConf.style.boxButton, onclick :+= {(e:Event) => BrowserConsole.log(ConditionValue("".asJson).asJson); e.preventDefault(); data.set(ConditionValue("".asJson).asJson)}),
        button("FieldRef",ClientConf.style.boxButton, onclick :+= ((e:Event) => data.set(ConditionFieldRef(columns.get.head).asJson))),
        button("Not",ClientConf.style.boxButton, onclick :+= ((e:Event) => data.set(NotCondition(EmptyCondition).asJson)))
      ) else Seq[Modifier]()

    )




    def renderCondition(data:Property[Json],firstLevel:Boolean = false):Modifier = {
      data.get.as[Condition].toOption match {
        case Some(c) => c match {
          case ConditionValue(_) => renderConditionValue(data)
          case ConditionFieldRef(_) => renderConditionFieldRef(data)
          case NotCondition(_) => renderNot(data)
          case OrCondition(_) => renderOr(data)
          case AndCondition(_) => renderAnd(data)
          case ConditionalField(_, _) => renderConditionalField(data)
          case EmptyCondition => emptyButtons(data, firstLevel)
        }
        case None if firstLevel && data.get.isNull => emptyButtons(data, firstLevel) //accept null as empty
        case None  => Seq[Modifier]()
      }
    }

    override protected def edit(nested: Binding.NestedInterceptor): JsDom.all.Modifier = {
      produce(params.prop) { data =>
        val _data = Property(data)
        registration.foreach(_.cancel())

        registration = Some(params.prop.sync(_data)(x => x, x => x))

        div(
          renderCondition(_data,firstLevel = true),

        ).render
      }
    }

  }
}
