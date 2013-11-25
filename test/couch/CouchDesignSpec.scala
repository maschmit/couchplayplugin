package couch

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

import models._

class CouchDesignSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val couch = Couch("http://localhost:5984/")
  val testDbName = "scala-couch-test"
  val testDb = couch.db(testDbName)

  val doc11 = Json.obj("_id" -> "1", "k" -> Seq(1, 1))
  val doc12 = Json.obj("_id" -> "2", "k" -> Seq(1, 2))
  val doc21 = Json.obj("_id" -> "3", "k" -> Seq(2, 1))
  val docs = Seq(doc11, doc12, doc21)
  val design = Json.obj(
    "views" -> Json.obj(
      "add" -> Json.obj(
        "map" -> """|function(doc) {
                    |  emit(doc.k, 1)
                    |}""".stripMargin
      )
    )
  )

  before {
    Await.ready(couch.addDb(testDbName), 1.second)
    docs.map(testDb.create(_)).map(f => Await.ready(f, 1.second))
    Await.ready(testDb.create("_design/test", design), 1.second)
  }
  after {
    Await.result(couch.removeDb(testDbName), 1.second)
  }

  "CouchDesign.view" should "produce a result for map views" in {
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
}
