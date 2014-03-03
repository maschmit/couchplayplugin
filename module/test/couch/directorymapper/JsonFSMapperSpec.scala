package couch.directorymapper

import java.io.File

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import play.api.libs.json._


class JsonFSMapperSpec extends FlatSpec with ShouldMatchers with GivenWhenThen {
  
  val testFileDir = new File("test/couch/testfiles/")
  def testFile(path: String) = new File(testFileDir, path)

  val mapper = JsonFSMapper

  "A file containing {}" should "map to {}" in {
    Given("a file containing an empty object")
    val file = testFile("emptyObject.json")
    When("it is read using the mapper")
    val value = mapper.load(file)
    Then("the value is an empty object")
    value should be (Json.obj())
  }
  
  "An empty directory" should "map to {}" in {
    Given("an empty directory")
    val file = testFile("emptyDirectory")
    When("it is read using the mapper")
    val value = mapper.load(file)
    Then("the value is an empty object")
    value should be (Json.obj())
  }

  "A directory containing an empty directory key.json" should " map to {\"key\": {}}" in {
    Given("an directory containing an empty dir key.json")
    val file = testFile("withEmptyJsonDir")
    When("it is read using the mapper")
    val value = mapper.load(file)
    Then("the value is {key:{}}}")
    value should be (Json.obj("key" -> Json.obj()))
  }

  "A directory containing a json file key.json with contents \"[]\"" should "map to {\"key\": []}" in {
    Given("an directory containing a file containing an empty array")
    val file = testFile("withJsonFile")
    When("it is read using the mapper")
    val value = mapper.load(file)
    Then("the value is {key:[]}")
    value should be (Json.obj("key" -> Json.arr()))
  }

  "A directory containing a file with other extension key.txt with contents \"value\"" should "map to {\"key\": \"value\"}" in {
    Given("an directory containing a file containing text")
    val file = testFile("withTxtFile")
    When("it is read using the mapper")
    val value = mapper.load(file)
    Then("the value is {key:\"value\"}")
    value should be (Json.obj("key" -> "value"))
  }
}
