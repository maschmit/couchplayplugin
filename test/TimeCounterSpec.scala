import org.scalatest._
import org.scalamock.scalatest.MockFactory

import play.api.test._
import play.api.test.Helpers._
import play.api.cache.Cache

import models.Counter

class TimeCounterSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with MockFactory {
  "Adding a time counter" should "make it appear on the main page" in new WithApplication {
  	  val c = countersController
  	  Given("no counter called 'testCounter' is on the list")
  	  contentAsString(c.index(FakeRequest())) should not include ("testCounter")
  	  When("a request is made to add a time counter")
  	  val addResponse = c.addCounter(FakeRequest().withFormUrlEncodedBody(("name", "testCounter")))
  	  status(addResponse) should be (SEE_OTHER)
  	  Then("a counter called 'testCounter' should be on the list")
  	  contentAsString(c.index(FakeRequest())) should include ("testCounter")
  }

  "Adding to a time counter" should "increment the total of that counter" in new WithApplication {
    val c = countersController
    Given("a counter called 'testCounter' exists")
    Cache.set("counters", List(Counter("testCounter")))
    contentAsString(c.index(FakeRequest())) should (include ("testCounter") and include ("no time"))
    When("a request is made to increment the counter")
    val incrementResponse = c.incrementCounter("testCounter")(FakeRequest().withFormUrlEncodedBody(("minutes", "3")))
    status(incrementResponse) should be (SEE_OTHER)
    Then("the counter aggregate is incremented")
    contentAsString(c.index(FakeRequest())) should include ("3 minutes")
  }

  def countersController = new controllers.CountersControllerTrait with repositories.CounterRepositoryComponent {
    val counterRepository = new CouchCounterRepository
  }
}
