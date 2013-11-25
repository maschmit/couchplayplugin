package couch

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class DocumentUpdateResult(ok: Boolean, id: String, rev: String)

case class Document(head: DocumentHeader, body: JsObject)

case class DocumentHeader(id: String, rev: String)

case class MapViewElement(id: String, key: JsValue, value: JsValue)

abstract class ViewResult
object ViewResult {
  def apply(total_rows: Option[Int], offset: Option[Int], rows: List[MapViewElement]) = (total_rows, offset) match {
  	case (Some(rc), Some(o)) => MapViewResult(rc, o, rows)
  }  
}

case class MapViewResult(rowCount: Int, offset: Int, rows: List[MapViewElement]) extends ViewResult


abstract class CouchError extends Exception {
	def error: String
	def reason: String
	override def getMessage: String = s"Error: '$error', Reason: '$reason'"
}
case class DocumentNotFound(override val error: String, override val reason: String) extends CouchError
case class DocumentCreationFailed(override val error: String, override val reason: String) extends CouchError
case class GeneralCouchError(override val error: String, override val reason: String) extends CouchError


object ImplicitReaders {
  implicit val mapViewElement: Reads[MapViewElement] = (
      (__ \ "id").read[String] ~
      (__ \ "key").read[JsValue] ~
      (__ \ "value").read[JsValue]
  	)(MapViewElement)

  implicit val viewResultReads: Reads[ViewResult] = (
      (__ \ "total_rows").readNullable[Int] ~
      (__ \ "offset").readNullable[Int] ~
      (__ \ "rows").read[List[MapViewElement]]
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
