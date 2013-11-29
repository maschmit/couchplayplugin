package couch.view

import ViewReaders._

import org.scalatest._
import play.api.libs.json._

class ViewReaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
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
