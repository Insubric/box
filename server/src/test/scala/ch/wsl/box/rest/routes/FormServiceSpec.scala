//package ch.wsl.box.rest
//
//import ch.wsl.box.model.shared.JSONField
//import org.junit.runner.RunWith
//
//
//class FormServiceSpec extends BaseSpec {
//
//
//  "Form masks" should {
//
//    "return the available form list" in {
//      get(endpoint + "/form") {
//        responseAs[Seq[String]] === Seq("test_form")
//      }
//    }
//
//    "return a form selected by id" in {
//      get(endpoint + "/form/test_form") {
//        val form = responseAs[Seq[JSONField]]
//        form(0).table === "a"
//        form(0).key === "test"
//      }
//    }
//
//  }
//
//}