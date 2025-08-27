package ch.wsl.box.model.shared

import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

sealed trait Condition {
  def check(data: Json): Boolean = Condition.check(this, data)
  def variables: Seq[String] = Condition.variables(this)
  def asString:String = this match {
    case ConditionValue(value) => value.string
    case ConditionFieldRef(valueField) => s"field: $valueField"
    case NotCondition(not) => s"not ${not.asString}"
    case OrCondition(or) => or.map(_.asString).mkString("["," or ", "]")
    case AndCondition(and) => and.map(_.asString).mkString("["," and ", "]")
    case ConditionalField(field, condition) => s"$field: ${condition.asString}"
    case EmptyCondition => "empty"
  }

}
case class ConditionValue(value: Json) extends Condition
case class ConditionFieldRef(valueField: String) extends Condition
case class NotCondition(not:Condition) extends Condition
case class OrCondition(or:Seq[Condition]) extends Condition
case class AndCondition(and:Seq[Condition]) extends Condition
case class ConditionalField(field:String,condition:Condition) extends Condition
case object EmptyCondition extends Condition

object Condition {

  def check(condition: Condition, data: Json): Boolean = {
    def _check(condition: Condition, _data: Json): Boolean = {
      condition match {
        case ConditionValue(value) => _data == value
        case ConditionFieldRef(valueField) => _data == data.js(valueField)
        case NotCondition(not) => _check(not, _data)
        case OrCondition(or) => or.exists(_check(_, _data))
        case AndCondition(and) => and.nonEmpty && and.forall(_check(_, _data))
        case ConditionalField(field, condition) => _check(condition, data.js(field))
        case EmptyCondition => false
      }
    }

    _check(condition, data)
  }

  def variables(condition: Condition):Seq[String] = {
      condition match {
        case ConditionValue(value) => Seq()
        case ConditionFieldRef(valueField) => Seq()
        case NotCondition(not) => variables(not)
        case OrCondition(or) => or.flatMap(variables)
        case AndCondition(and) => and.flatMap(variables)
        case ConditionalField(field, condition) => Seq(field) ++ variables(condition)
        case EmptyCondition => Seq()
    }
  }


  implicit val encoderEmpty = new Encoder[EmptyCondition.type] {
    override def apply(a: EmptyCondition.type): Json = Json.Null
  }



  implicit val encoder= new Encoder[Condition] {
    override def apply(a: Condition): Json = {
      a match {
        case ConditionValue(value) => Json.obj("value" -> value)
        case ConditionFieldRef(valueField) => Json.obj("valueField" -> Json.fromString(valueField))
        case NotCondition(not) => Json.obj("not" -> apply(not))
        case OrCondition(or) => Json.obj("or" -> Json.fromValues(or.map(apply)))
        case AndCondition(and) => Json.obj("and" -> Json.fromValues(and.map(apply)))
        case ConditionalField(field, condition) => Json.obj("field" -> Json.fromString(field), "condition" -> apply(condition))
        case EmptyCondition => Json.Null
      }
    }
  }

  implicit val decoder = new Decoder[Condition] {
    override def apply(c: HCursor): Result[Condition] = {
      if(c.value.isObject) {
        c.downField("value").as[Json].map(x => ConditionValue(x))
          .orElse{c.downField("valueField").as[String].map(x => ConditionFieldRef(x))}
          .orElse{c.downField("not").as[Json].flatMap(x => apply(x.hcursor).map(NotCondition(_)))}
          .orElse{c.downField("or").as[Seq[Json]].map(x => OrCondition(x.flatMap( o => apply(o.hcursor).toOption))) }
          .orElse{c.downField("and").as[Seq[Json]].map(x => AndCondition(x.flatMap( o => apply(o.hcursor).toOption))) }
          .orElse{
            for{
              f <- c.downField("field").as[String]
              c <- c.downField("condition").as[Json]
              cond <- apply(c.hcursor)
            } yield ConditionalField(f,cond)
          }
      } else if(c.value.isNull) Right(EmptyCondition) else Left(DecodingFailure(s"Condition JSON not valid ${c.value}",List()))
    }
  }

  import io.circe.generic.semiauto._
  implicit val cvEncoder: Encoder[ConditionValue] = new Encoder[ConditionValue] {
    override def apply(a: ConditionValue): Json = Json.obj("value" -> a.value)
  }
  implicit val cfrEncoder: Encoder.AsObject[ConditionFieldRef] = deriveEncoder[ConditionFieldRef]
  implicit val ncEncoder: Encoder.AsObject[NotCondition] = deriveEncoder[NotCondition]
  implicit val ocEncoder: Encoder.AsObject[OrCondition] = deriveEncoder[OrCondition]
  implicit val acEncoder: Encoder.AsObject[AndCondition] = deriveEncoder[AndCondition]
  implicit val cfEncoder: Encoder.AsObject[ConditionalField] = deriveEncoder[ConditionalField]
  implicit val cvDecoder: Decoder[ConditionValue] = new Decoder[ConditionValue] {
    override def apply(c: HCursor): Result[ConditionValue] = Right(ConditionValue(c.value.js("value")))
  }
  implicit val cfrDecoder: Decoder[ConditionFieldRef] = deriveDecoder[ConditionFieldRef]
  implicit val ncDecoder: Decoder[NotCondition] = deriveDecoder[NotCondition]
  implicit val ocDecoder: Decoder[OrCondition] = deriveDecoder[OrCondition]
  implicit val acDecoder: Decoder[AndCondition] = deriveDecoder[AndCondition]
  implicit val cfDecoder: Decoder[ConditionalField] = deriveDecoder[ConditionalField]

  def fromJson(json: Json):Condition = json.as[Condition] match {
    case Left(value) => throw new Exception(s"Json for condition not valid $json error $value")
    case Right(value) => value
  }


  def in(field:String,values:Seq[Json]):Condition = ConditionalField(field,OrCondition(values.map(ConditionValue)))
  def inStr(field:String,values:Seq[String]):Condition = in(field,values.map(Json.fromString))

}
