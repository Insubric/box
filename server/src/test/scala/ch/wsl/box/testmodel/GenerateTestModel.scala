//package ch.wsl.box.testmodel
//
//import ch.wsl.box.codegen.{CodeGenerator, GeneratorParams}
//import ch.wsl.box.rest.BaseSpec
//import org.scalatest.Assertion
//
//
//class GenerateTestModel extends BaseSpec {
//
//  "App managed form"  should "insert a single layer json"  in withServices[Assertion]{ services =>
//    val params = GeneratorParams(Seq("*"),Seq("*"),Seq(),Seq())
//    println(CodeGenerator("public",services.connection,params).generatedFiles().entities.code)
//    assert(true)
//  }
//
//
//
//}
