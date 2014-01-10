package couch.error

import ErrorReaders._

import org.scalatest._
import play.api.libs.json._

class ErrorReaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
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
