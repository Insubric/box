package ch.wsl.box.client.forms.table

import ch.wsl.box.client.geo.Control
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, EntityTableState, Main, TestBase}
import ch.wsl.box.model.shared.{IDs, JSONField, JSONMetadata, JSONQuery}
import org.scalajs.dom.{HTMLInputElement, document}
import org.scalatest.Assertion
import scribe.Level

import java.util.UUID
import scala.concurrent.{Future, Promise}
import scala.util.Try

class SimpleTableFilterTest extends TestBase {

  override val debug: Boolean = false
  override val waitOnAssertFail: Boolean = true

  val testValue = "1"
  var checkQuery = false
  val promise = Promise[Assertion]()

  override def loggerLevel: Level = Level.Debug

  val formName = "formName"

  class MockValues extends Values(loggerLevel){
    override def metadata: JSONMetadata = JSONMetadata.simple(UUID.randomUUID(),"form",formName,"it",Seq(
      JSONField.integer("id"),
      JSONField.string("label")
    ),Seq("id"))

    override def ids: IDs = {
      IDs(true,1,Seq("id::1","id::2"),2)
    }

    override def csv(q: JSONQuery): Seq[Seq[String]] = {
      if(checkQuery) {
        promise.tryComplete { Try{
          q.filter.nonEmpty shouldBe true
          q.filter.head.column shouldBe "id"
          q.filter.head.value shouldBe Some(testValue)

        }}
      }
      Seq(
        Seq("1","bla"),
        Seq("2","bla2")
      )
    }
  }

  override def values: Values = new MockValues

  "Table" should "be filtered by column" in test {
    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityTableState("form", formName, None, false))
      }
      _ <- waitElement(() => document.querySelector(s"tbody tr"),"Waiting some rows")
      _ <- Future{
        val filterField = document.querySelector(s"thead input").asInstanceOf[HTMLInputElement]
        checkQuery = true
        filterField.value = testValue
        filterField.onchange(null)
      }
      result <- promise.future
    } yield result
  }
}