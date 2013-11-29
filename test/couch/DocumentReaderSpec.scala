package couch.document

import DocumentReaders._

import org.scalatest._
import play.api.libs.json._

class DocumentReaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
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
}
