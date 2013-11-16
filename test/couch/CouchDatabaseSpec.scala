import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

import models._

class CouchDatabaseSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with MockFactory with BeforeAndAfterEach {
  val couch = Couch.url("http://localhost:5984/")
  val testDbName = "scala-couch-test"
  val testDb = couch.db(testDbName)

  override def beforeEach = Await.result(couch.addDb(testDbName), 1.second)
  override def afterEach = Await.result(couch.removeDb(testDbName), 1.second)

  "CouchDatabase.create" should "succeed and respond with the meta info about the document which can be used to fetch the document again" in {
    When("a doc is added to a db")
    val testDoc = Json.obj("string" -> "test document")
	val result = Await.result(testDb.create(testDoc), 1.second)
	Then("the response should be successful")
	result.isInstanceOf[SuccessfulUpdateResponse] should be (true)
	val success = result.asInstanceOf[SuccessfulUpdateResponse]
	And("the response should contain an id which can be used to retrieve the document")
	val doc = Await.result(testDb.get(success.head.id), 1.second).asInstanceOf[Document]
	doc.body.toString should include ("\"string\":\"test document\"")
  }
}
