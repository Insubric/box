package ch.wsl.box.rest

import ch.wsl.box.testmodel.Entities._
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.auto._
import ch.wsl.box.BaseSpec

class ListTypesSpec extends BaseSpec {

  val testTextList = Some(List("blabla"))
  val testIntList = List(1,2)
  val testDoubleList = List(1.0,2.0)

  "List type"  should "be handled"  in withServices[Assertion] { implicit services =>

    def insert = services.connection.dbConnection.run {
      Test_list_types += Test_list_types_row(Some(1),testTextList)
    }

    def fetch = services.connection.dbConnection.run {
      Test_list_types.result
    }

    def selectLight = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.doSelectLight(sql"")
    }


    for {
      countInsert <- insert
      entries <- fetch
      entriesLight <- selectLight
    } yield {
      countInsert shouldBe 1
      entries.length shouldBe 1
      entries.head.texts shouldBe testTextList
      entriesLight.length shouldBe 1
      entriesLight.head.texts shouldBe testTextList
    }

  }

  it should "update list of ints"  in withServices[Assertion] { implicit services =>

    def insert = services.connection.dbConnection.run {
      Test_list_types += Test_list_types_row(Some(1))
    }


    def selectLight = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.doSelectLight(sql"")
    }

    def update(id:Int) = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.updateReturning(Map("ints" -> testIntList.asJson),Map("id" -> Json.fromInt(id)))
    }

    for {
      countInsert <- insert
      entriesLight <- selectLight
      u <- update(entriesLight.head.id.get)
      entriesAfterUpdate <- selectLight
    } yield {
      countInsert shouldBe 1

      u.isDefined shouldBe true
      u.get.ints shouldBe Some(testIntList)
      entriesAfterUpdate.length shouldBe 1
      entriesAfterUpdate.head.ints shouldBe Some(testIntList)
    }

  }

  it should "update list of texts"  in withServices[Assertion] { implicit services =>

    def insert = services.connection.dbConnection.run {
      Test_list_types += Test_list_types_row(Some(1))
    }


    def selectLight = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.doSelectLight(sql"")
    }

    def update(id:Int) = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.updateReturning(Map("texts" -> testTextList.get.asJson),Map("id" -> Json.fromInt(id)))
    }

    for {
      countInsert <- insert
      entriesLight <- selectLight
      u <- update(entriesLight.head.id.get)
      entriesAfterUpdate <- selectLight
    } yield {
      countInsert shouldBe 1

      u.isDefined shouldBe true
      u.get.texts shouldBe testTextList
      entriesAfterUpdate.length shouldBe 1
      entriesAfterUpdate.head.texts shouldBe testTextList
    }

  }

  it should "update list of doubles"  in withServices[Assertion] { implicit services =>

    def insert = services.connection.dbConnection.run {
      Test_list_types += Test_list_types_row(Some(1))
    }


    def selectLight = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.doSelectLight(sql"")
    }

    def update(id:Int) = services.connection.dbConnection.run {
      Test_list_types.baseTableRow.updateReturning(Map("numbers" -> testDoubleList.asJson),Map("id" -> Json.fromInt(id)))
    }

    for {
      countInsert <- insert
      entriesLight <- selectLight
      u <- update(entriesLight.head.id.get)
      entriesAfterUpdate <- selectLight
    } yield {
      countInsert shouldBe 1

      u.isDefined shouldBe true
      u.get.numbers shouldBe Some(testDoubleList)
      entriesAfterUpdate.length shouldBe 1
      entriesAfterUpdate.head.numbers shouldBe Some(testDoubleList)
    }

  }

}
