package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.JSONQueryFilter.WHERE
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.testmodel.Entities.{Json_test, Json_test_row, Simple, Simple_row}
import ch.wsl.box.jdbc.PostgresProfile.api._

class FiltersSpec extends BaseSpec{

  "Find simple" should "filter list" in withServices { implicit services =>

    val dbio =  for{
      _ <- Simple += Simple_row(name = Some("asd"))
      result <- Registry().actions("simple").findSimple(JSONQuery.filterWith(WHERE.in("name",Seq("asd"))))
    } yield result


    services.connection.dbConnection.run(dbio).map{ result =>
      result.isEmpty shouldBe false
    }
  }

  it should "filter with not" in withServices { implicit services =>

    val dbio =  for{
      _ <- Simple += Simple_row(name = Some("asd"))
      result <- Registry().actions("simple").findSimple(JSONQuery.filterWith(WHERE.not("name","asd")))
    } yield result


    services.connection.dbConnection.run(dbio).map{ result =>
      result.isEmpty shouldBe true
    }
  }

  it should "filter ignore empty string filters when not string" in withServices { implicit services =>

    val dbio =  for{
      _ <- Json_test += Json_test_row(id = 1)
      result <- Registry().actions("json_test").findSimple(JSONQuery.filterWith(WHERE.in("id",Seq(""))))
    } yield result


    services.connection.dbConnection.run(dbio).map{ result =>
      result.isEmpty shouldBe false
    }
  }

}
