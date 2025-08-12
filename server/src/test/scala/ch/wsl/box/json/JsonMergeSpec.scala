package ch.wsl.box.json

import ch.wsl.box.BaseSpec
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

class JsonMergeSpec extends BaseSpec {

  import Fixtures._

  "Json Merge" should "behave as deepMerge" in {
    json1.deepMerge(json2) shouldBe json1.merge(mainMetadata,Seq(child1Metadata,child2Metadata))(json2)
    json2.deepMerge(json1) shouldBe json2.merge(mainMetadata,Seq(child1Metadata,child2Metadata))(json1)
  }

  it should "merge json type as atomic type" in {
    complexObj1.merge(complexTypeMetadata)(complexObj2) shouldBe complexObj2
    complexObj2.deepMerge(complexObj1) shouldBe complexObj1
    complexObj2.merge(complexTypeMetadata)(complexObj1) shouldBe complexObj1
    complexObj2.merge(complexTypeMetadata)(complexObj2) shouldBe complexObj2
  }

  it should "merge array type as atomic type" in {
    listObj1.merge(listTypeMetadata)(listObj2) shouldBe listObj2
    listObj2.merge(listTypeMetadata)(listObj1) shouldBe listObj1
  }

}
