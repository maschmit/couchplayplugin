package couch

import play.api.libs.json._
import play.api.libs.functional.syntax._

import ImplicitReaders._

case class DocumentUpdateResult(ok: Boolean, id: String, rev: String)

case class Document(head: DocumentHeader, body: JsObject)

case class DocumentHeader(id: String, rev: String)

abstract class ViewElement {
	def key: JsValue
	def value: JsValue
}
case class MapViewElement(id: String, override val key: JsValue, override val value: JsValue) extends ViewElement
case class ReduceViewElement(override val key: JsValue, override val value: JsValue) extends ViewElement

abstract class ViewResult {
  type Element <: ViewElement
  def rows: List[Element]
  protected lazy val keysMulti: Map[JsValue, Iterable[Element]] = 
    rows.groupBy(_.key)
}
object ViewResult {
  def apply(total_rows: Option[Int], offset: Option[Int], rows: JsArray) = (total_rows, offset) match {
  	case (Some(rc), Some(o)) => MapViewResult(rc, o, rows.as[List[MapViewElement]])
  	case (None, None)        => ReduceViewResult(rows.as[List[ReduceViewElement]])
  }  
}
case class MapViewResult(rowCount: Int, offset: Int, override val rows: List[MapViewElement]) extends ViewResult {
  type Element = MapViewElement
}
case class ReduceViewResult(override val rows: List[ReduceViewElement]) extends ViewResult {
  type Element = ReduceViewElement
  def key(k: JsValue): ReduceViewElement = keysMulti(k).iterator.next
}


abstract class CouchError extends Exception {
	def error: String
	def reason: String
	override def getMessage: String = s"Error: '$error', Reason: '$reason'"
}
case class DocumentNotFound(override val error: String, override val reason: String) extends CouchError
case class DocumentCreationFailed(override val error: String, override val reason: String) extends CouchError
case class GeneralCouchError(override val error: String, override val reason: String) extends CouchError


object ImplicitReaders {
  implicit val reduceViewElement: Reads[ReduceViewElement] = (
      (__ \ "key").read[JsValue] ~
      (__ \ "value").read[JsValue]
  	)(ReduceViewElement)

  implicit val mapViewElement: Reads[MapViewElement] = (
      (__ \ "id").read[String] ~
      (__ \ "key").read[JsValue] ~
      (__ \ "value").read[JsValue]
  	)(MapViewElement)

  implicit val viewResultReads: Reads[ViewResult] = (
      (__ \ "total_rows").readNullable[Int] ~
      (__ \ "offset").readNullable[Int] ~
      (__ \ "rows").read[JsArray]
  	)(ViewResult.apply _)

  implicit val documentCreateResult: Reads[DocumentUpdateResult] = (
  	(__ \ "ok").read[Boolean] ~
    (__ \ "id").read[String] ~
    (__ \ "rev").read[String]
    )(DocumentUpdateResult)

  implicit val documentHeader: Reads[DocumentHeader] = (
    (__ \ "_id").read[String] ~
    (__ \ "_rev").read[String]
    )(DocumentHeader)

  implicit val document: Reads[Document] = (
    documentHeader ~
    (__).read[JsObject]
  )(Document)

  private def couchErrorReads = (
  	(__ \ "error").read[String] ~
  	(__ \ "reason").read[String]
  )

  implicit val docNotFoundReads: Reads[DocumentNotFound] =
    couchErrorReads(DocumentNotFound)

  implicit val docCreationFailedReads: Reads[DocumentCreationFailed] =
    couchErrorReads(DocumentCreationFailed)

  implicit val generalErrorReads: Reads[GeneralCouchError] =
    couchErrorReads(GeneralCouchError)
}
