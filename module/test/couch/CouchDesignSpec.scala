package couch

import sync.CouchDesignDocument

import view._
import test.DatabaseForEach

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


class CouchDesignSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with DatabaseForEach {
  val testRoot = "test/couch/testfiles/"
  def testPath(path: String) = testRoot + path

  val doc11 = Json.obj("_id" -> "1", "k" -> Seq(1, 1))
  val doc12 = Json.obj("_id" -> "2", "k" -> Seq(1, 2))
  val doc21 = Json.obj("_id" -> "3", "k" -> Seq(2, 1))
  val docs = Seq(doc11, doc12, doc21)

  val mapDesign = CouchDesignDocument.read(testPath("designdocs/_design/mapDoc.json")).json
  val mapReduceDesign = CouchDesignDocument.read(testPath("designdocs/_design/mapReduceDoc.json")).json


  override def beforeWithDatabase() =
    docs.map(testDb.create(_)).map(f => Await.result(f, 1.second))

  "CouchDesign.view" should "produce a result for map views" in {
    Given("a valid map view")
    Await.result(testDb.create("_design/test", mapDesign), 1.second)
    When("the view is requested plainly")
    val result = Await.result(testDb.design("test").view("add").get, 1.second)
    Then("the result should be a map result")
    var mapresult = result.asInstanceOf[MapViewResult]
    And("the result should have the correct values")
    mapresult.rowCount should be (3)
    mapresult.offset should be (0)
    mapresult.rows(0) should be (MapViewElement("1", Json.arr(1, 1), JsNumber(1)))
    mapresult.rows(1) should be (MapViewElement("2", Json.arr(1, 2), JsNumber(1)))
    mapresult.rows(2) should be (MapViewElement("3", Json.arr(2, 1), JsNumber(1)))
  }

  it should "produce a map result from a view with reduce if requested" in {
    Given("a valid map and reduce view")
    Await.ready(testDb.create("_design/test", mapReduceDesign), 1.second)
    When("the view is requested with noReduce")
    val result = Await.result(testDb.design("test").view("add").notReduced.get, 1.second)
    Then("the result should have the correct values")
    result.rowCount should be (3)
    result.offset should be (0)
    result.rows(0) should be (MapViewElement("1", Json.arr(1, 1), JsNumber(1)))
    result.rows(1) should be (MapViewElement("2", Json.arr(1, 2), JsNumber(1)))
    result.rows(2) should be (MapViewElement("3", Json.arr(2, 1), JsNumber(1)))
  }

  it should "produce a result for reduce views" in {
    Given("a valid map and reduce view")
    Await.ready(testDb.create("_design/test", mapReduceDesign), 1.second)
    When("the view is requested plainly")
    val result = Await.result(testDb.design("test").view("add").reduced.get, 1.second)
    Then("the result should have the correct values")
    result.rows(0) should be (ReduceViewElement(JsNull, JsNumber(3)))
  }

  it should "optionally be able to request grouping for reduce views" in {
    Given("a valid map and reduce view")
    Await.ready(testDb.create("_design/test", mapReduceDesign), 1.second)
    When("the view is requested grouped")
    val result = Await.result(testDb.design("test").view("add").grouped.reduced.get, 1.second)
    Then("the result should have the correct values")
    result.key(Json.arr(1, 1)) should be (ReduceViewElement(Json.arr(1, 1), JsNumber(1)))
    result.key(Json.arr(1, 2)) should be (ReduceViewElement(Json.arr(1, 2), JsNumber(1)))
    result.key(Json.arr(2, 1)) should be (ReduceViewElement(Json.arr(2, 1), JsNumber(1)))
  }
}
