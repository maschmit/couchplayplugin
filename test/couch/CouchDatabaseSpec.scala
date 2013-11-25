package couch

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

import models._

class CouchDatabaseSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfterEach {
  val couch = Couch("http://localhost:5984/")
  val testDbName = "scala-couch-test"
  val testDb = couch.db(testDbName)

  val testDoc = Json.obj("string" -> "test document")

  override def beforeEach = Await.result(couch.addDb(testDbName), 1.second)
  override def afterEach = Await.result(couch.removeDb(testDbName), 1.second)

  "CouchDatabase.create" should "succeed and respond with the meta info about the document which can be used to fetch the document again" in {
    When("a doc is added to a db")
  	val result = Await.result(testDb.create(testDoc), 1.second)
	  Then("the response should be successful")
	  And("the response should contain an id which can be used to retrieve the document")
	  val doc = Await.result(testDb.get(result.id), 1.second).asInstanceOf[Document]
	  doc.body.toString should include ("\"string\":\"test document\"")
  }

  it should "fail and throw an exception if the database does not exist" in {
    intercept[CouchError] {
      Await.result(couch.db("non-existent-db").create(testDoc), 1.second)
    }
  }

  "CouchDatabase.create(id)" should "create a document with that id" in {
    When("a doc is added to a db at a particular id")
    val result = Await.result(testDb.create("theid", testDoc), 1.second)
    Then("the response should be successful")
    result.isInstanceOf[DocumentUpdateResult] should be (true)
    And("the doc should be retrieveable using that id")
    val doc = Await.result(testDb.get("theid"), 1.second).asInstanceOf[Document]
    doc.body.toString should include ("\"string\":\"test document\"")
  }

  "CouchDatabase.get" should "succeed for an existing document" in {
    Given("a doc is added")
    val testDoc = Json.obj("_id" -> "theid", "string" -> "test document")
    Await.result(testDb.create(testDoc), 1.second)
    When("the doc is requested")
    val result = Await.result(testDb.get("theid"), 1.second)
    Then("that will be the original document")
    (result.body \ "string").as[String] should be ("test document")
  }

  it should "fail if the document does not exist" in {
    When("a non existent doc is requested")
    intercept[DocumentNotFound] {
      Await.result(testDb.get("theid"), 1.second)
    }
  }
}