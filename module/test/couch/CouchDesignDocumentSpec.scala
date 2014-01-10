package couch

import sync.CouchDesignDocument

import view._

import org.scalatest._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import play.api.libs.json._

class CouchDesignDocumentSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {

  val mapReduceDesign = CouchDesignDocument(Json.obj(
    "views" -> Json.obj(
      "add" -> Json.obj(
        "map" -> JsString("""|function(doc) {
                    |  emit(doc.k, 1)
                    |}""".stripMargin), 
        "reduce" -> JsString("""|function(key, values, rereduce) {
                      |  return sum(values);
                      |}""".stripMargin)
      )
    )
  ))

  "CouchDesignDocument.read" should "read a design document from a local file" in {
    When("the document is read")
    val doc = CouchDesignDocument.read("test/couch/designs/mapReduceDoc.json")
    Then("the result should be a design document")
    (doc.isInstanceOf[CouchDesignDocument]) should be (true)
    And("the result should have the correct values")
    doc should be (mapReduceDesign)
  }
}
