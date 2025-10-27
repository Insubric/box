package ch.wsl.box.shared

import ch.wsl.box.BaseSpec
import _root_.io.circe.parser._
import ch.wsl.box.model.shared.{AndCondition, Condition, ConditionFieldRef, ConditionValue, ConditionalField, EmptyCondition, NotCondition, OrCondition}
import io.circe.Json

class ConditionalSpec extends BaseSpec{

  "Conditional JSON" should "be parsed" in {
    val json = parse(s"""
       |{
       |    "field" : "test_id",
       |    "condition" : {
       |      "not" : {"value": "020202"}
       |    }
       |  }
       |""".stripMargin).toOption.get

    json.as[Condition] match {
      case Left(value) => fail(value.message)
      case Right(condition) => {
        //check parsing
        condition match {
          case ConditionalField(field, condition) => {
            field shouldBe "test_id"
            condition match {
              case NotCondition(not) => {
                not match {
                  case ConditionValue(value) => value shouldBe Json.fromString("020202")
                  case _ => fail()
                }
              }
              case _ => fail()
            }
          }
          case _ => fail()

        }

        condition.check(Json.obj("test_id" -> Json.fromString("020202"))) shouldBe false
        condition.check(Json.obj("test_id" -> Json.fromString("0000"))) shouldBe true





      }
    }


  }

}
