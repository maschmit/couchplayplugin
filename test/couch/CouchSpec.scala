import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import models._

class CouchSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with MockFactory {
	val dbUrl = "http://localhost:5984/"
	val testDb = "scala-couch-test"

	"Couch.addDb" should "succeed if that db has just been removed" in {
		Given("a couch instance")
		val couch = Couch.url(dbUrl)
		When("test db is removed")
		Await.result(couch.removeDb(testDb), 1.second)
		And("test db is added again")
		val result = Await.result(couch.addDb(testDb), 1.second)
		Then("The response should be successful")
		result should be (true)
	}
}
