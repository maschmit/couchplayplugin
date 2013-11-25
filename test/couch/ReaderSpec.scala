
import org.scalatest._
import play.api.libs.json._
import couch._
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
}
