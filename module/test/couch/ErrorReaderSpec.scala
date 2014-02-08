package couch.error

import ErrorReaders._

import org.scalatest._
import play.api.libs.json._

class ErrorReaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
  "CouchErrorInfo" should "be deserialised from a couch failure message" in {
  	Given("a properly formed couchdb error response")
    val json = Json.obj("error" -> "error name", "reason" -> "reason code")
    When("it is converted to a DocumentNotFound")
    val ch = json.as[CouchErrorInfo]
    Then("it should be properly formed")
    ch should be (CouchErrorInfo("error name", "reason code"))
  }
}
