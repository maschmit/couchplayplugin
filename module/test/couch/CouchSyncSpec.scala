package couch

import sync.CouchDesignDocument
import sync.CouchSync

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

class CouchSyncSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val couch = Couch("http://localhost:5984/")
  val testDbName = "scala-couch-test"
  val testDb = couch.db(testDbName)
  val docPtr = testDb.doc("docId")


  val mapDesign = CouchDesignDocument.read("test/couch/designs/mapDoc.json").json
  val mapReduceDesign = CouchDesignDocument.read("test/couch/designs/mapReduceDoc.json").json

  before {
    Await.ready(couch.addDb(testDbName), 1.second)
  }
  after {
    Await.result(couch.removeDb(testDbName), 1.second)
  }

  "CouchSync(Json).to(DocumentPointer)" should "create a doc if it exists locally and not in the db" in {
    When("a new doc is synced")
    Await.result(CouchSync(mapDesign).to(docPtr), 1.second)
    Then("the doc should exist")
    val document = Await.result(docPtr.get(), 1.second)
    document.body should be (mapDesign)
  }

  it should "do nothing if an identical doc already exists" in {
    Given("an existing doc")
    val creationResult = Await.result(docPtr.create(mapDesign), 1.second)
    When("an identical doc is synced")
    Await.result(CouchSync(mapDesign).to(docPtr), 1.second)
    Then("a new revision shouldn't be created")
    val document = Await.result(docPtr.get(), 1.second)
    document.head should be (creationResult.head)
  }

  it should "replace an existing doc if it has different contents" in {
    Given("an existing doc")
    Await.result(docPtr.create(mapReduceDesign), 1.second)
    When("an identical doc is synced")
    Await.result(CouchSync(mapDesign).to(docPtr), 1.second)
    Then("the doc should contain the new version")
    val document = Await.result(docPtr.get(), 1.second)
    document.body should be (mapDesign)
  }
}
