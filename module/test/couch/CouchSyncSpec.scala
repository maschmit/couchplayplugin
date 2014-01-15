package couch

import sync.CouchDesignDocument
import sync._

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

import java.io.File


class CouchSyncSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val couch = Couch("http://localhost:5984/")
  val testDbName = "scala-couch-test"
  val testDb = couch.db(testDbName)
  val docPtr = testDb.doc("docId")

  val mapDesign = CouchDesignDocument.read("test/couch/testfiles/_design/mapDoc.json").json
  val mapReduceDesign = CouchDesignDocument.read("test/couch/testfiles/_design/mapReduceDoc.json").json

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
    When("a differing doc is synced")
    Await.result(CouchSync(mapDesign).to(docPtr), 1.second)
    Then("the doc should contain the new version")
    val document = Await.result(docPtr.get(), 1.second)
    document.body should be (mapDesign)
  }

  "CouchSync(directoryPath).to(Database)" should "create docs if they exist locally and not in the db" in {
    When("a new dir is synced")
    val syncFutures = CouchSync(new File("test/couch/testfiles/_design")).to(testDb)
    syncFutures.foreach(Await.result(_, 1.second))
    Then("the docs should exist")
    val doc1 = Await.result(testDb.get("mapDoc"), 1.second)
    doc1.body should be (mapDesign)
    val doc2 = Await.result(testDb.get("mapReduceDoc"), 1.second)
    doc2.body should be (mapReduceDesign)
  }

  it should "sync files in _design/ to design documents" in {
    When("a new dir contating _design dir is synced")
    val syncFutures = CouchSync(new File("test/couch/testfiles")).to(testDb)
    syncFutures.foreach(Await.result(_, 1.second))
    Then("the docs should exist")
    val doc1 = Await.result(testDb.get("_design/mapDoc"), 1.second)
    doc1.body should be (mapDesign)
    val doc2 = Await.result(testDb.get("_design/mapReduceDoc"), 1.second)
    doc2.body should be (mapReduceDesign)
  }

  "CouchSync(Json).check(DocumentPointer)" should "not match if it exists locally and not in the db" in {
    When("a new doc is checked")
    val result = Await.result(CouchSync(mapDesign).check(docPtr), 1.second)
    Then("the result should be not matched")
    result should not be a ('match)
  }

  it should "match if an identical doc already exists" in {
    Given("an existing doc")
    val creationResult = Await.result(docPtr.create(mapDesign), 1.second)
    When("an identical doc is checked")
    val result = Await.result(CouchSync(mapDesign).check(docPtr), 1.second)
    Then("the result should be a match")
    result should be a ('match)
  }

  it should "not match if an existing doc has different contents" in {
    Given("an existing doc")
    Await.result(docPtr.create(mapReduceDesign), 1.second)
    When("a differing doc is checked")
    val result = Await.result(CouchSync(mapDesign).check(docPtr), 1.second)
    Then("the result should not be a match")
    result should not be a ('match)
  }

  "CouchSync(directoryPath).check(Database)" should "not match if they exist locally and not in the db" in {
    When("a new dir is checked")
    val result = Await.result(CouchSync(new File("test/couch/testfiles")).check(testDb), 1.second)
    Then("the result should be not matched")
    result should not be a ('match)
  }

  it should "match if the files have already been synced" in {
    Given("the dir is already synced")
    val syncFutures = CouchSync(new File("test/couch/testfiles")).to(testDb)
    syncFutures.foreach(Await.result(_, 1.second))
    When("the dir is checked")
    val result = Await.result(CouchSync(new File("test/couch/testfiles")).check(testDb), 1.second)
    Then("the result should be matched")
    result should be a ('match)
  }
}
