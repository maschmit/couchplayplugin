package couch

import org.scalatest._
import play.api.libs.json._
import ImplicitReaders._

class ReaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
  "DocumentHeader" should "be deserialised from a doc with a proper header" in {
  	Given("a properly formed couchdb header")
    val json = Json.obj("_id" -> "the id", "_rev" -> "revision")
    When("it is converted to a CouchHeader")
    val ch = json.as[DocumentHeader]
    Then("it should be properly formed")
    ch should be (DocumentHeader("the id", "revision"))
  }

  "DocumentUpdateResult" should "be deserialised from a successful creation response" in {
  	Given("a properly formed couchdb creation response")
    val json = Json.obj("ok" -> true, "id" -> "the id", "rev" -> "revision")
    When("it is converted to a DocumentUpdateResult")
    val ch = json.as[DocumentUpdateResult]
    Then("it should be properly formed")
    ch should be (DocumentUpdateResult(true, "the id", "revision"))
  }

  "DocumentNotFound" should "be deserialised from a couch failure message" in {
  	Given("a properly formed couchdb error response")
    val json = Json.obj("error" -> "error name", "reason" -> "reason code")
    When("it is converted to a DocumentNotFound")
    val ch = json.as[DocumentNotFound]
    Then("it should be properly formed")
    ch should be (DocumentNotFound("error name", "reason code"))
  }

  "DocumentCreationFailed" should "be deserialised from a couch failure message" in {
  	Given("a properly formed couchdb error response")
    val json = Json.obj("error" -> "error name", "reason" -> "reason code")
    When("it is converted to a DocumentCreationFailed")
    val ch = json.as[DocumentCreationFailed]
    Then("it should be properly formed")
    ch should be (DocumentCreationFailed("error name", "reason code"))
  }

  "ViewResult" should "be deserialised from a map view response" in {
    Given("a properly formed map view response")
    val json = Json.obj(
      "total_rows" -> 2,
      "offset" -> 1,
      "rows" -> Json.arr(
        Json.obj("id" -> "id1", "key" -> "key1", "value" -> Json.obj("key" -> "val")),
        Json.obj("id" -> "id2", "key" -> "key2", "value" -> Json.obj("key" -> "val")),
        Json.obj("id" -> "id3", "key" -> "key3", "value" -> Json.obj("key" -> "val"))
        ))
    When("it is converted to a ViewResult")
    val ch = json.as[ViewResult]
    Then("it should be properly formed")
    ch should be (MapViewResult(2, 1, List(
      MapViewElement("id1", JsString("key1"), Json.obj("key" -> "val")),
      MapViewElement("id2", JsString("key2"), Json.obj("key" -> "val")),
      MapViewElement("id3", JsString("key3"), Json.obj("key" -> "val"))
    )))
  }
}
