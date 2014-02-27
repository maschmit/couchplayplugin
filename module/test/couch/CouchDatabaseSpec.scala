package couch

import couch.test.DatabaseForEach
import document._
import error._

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class CouchDatabaseSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with DatabaseForEach {
  val testDoc = Json.obj("string" -> "test document")

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
      Await.result(absentDb.create(testDoc), 1.second)
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

  "CouchDatabase.delete" should "succeed for an existing document" in {
    Given("a doc is added")
    val creationResult = Await.result(testDb.create(testDoc), 1.second)
    When("the doc is deleted")
    val result = Await.result(testDb.delete(creationResult), 1.second)
    Then("there will be no document at that id")
    intercept[DocumentNotFound] {
      Await.result(testDb.get("theid"), 1.second)
    }
  }

  it should "fail if the document does not exist" in {
    When("a non existent doc is deleted")
    intercept[DocumentNotFound] {
      Await.result(testDb.delete(DocumentHeaderImpl("theid", "rev")), 1.second)
    }
  }

  "CouchDatabase.replace" should "be able to replace a doc if it knows the last revision number" in {
    Given("a document exists")
    val creationResult = Await.result(testDb.create(testDoc), 1.second)
    When("the document is replaced")
    val newDoc = Json.obj("new" -> "replaced")
    val result = Await.result(testDb.replace(creationResult, newDoc), 1.second)
    And("the doc is requested")
    val doc = Await.result(testDb.get(result.id), 1.second)
    Then("that will be the new document")
    (doc.body \ "new").as[String] should be ("replaced")
  }

  "CouchDatabase.info" should "succeed for an existing database" in {
    Given("a db exists")
    When("the info is requested")
    val result = Await.result(testDb.info(), 1.second)
    Then("it will succeed")
    result should be (DatabaseInfo())
  }

  it should "fail if the database does not exist" in {
    When("a non existent database is requested")
    intercept[DatabaseNotFound] {
      Await.result(absentDb.info, 1.second)
    }
  }
}
