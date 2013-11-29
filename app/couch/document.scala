package couch.document

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class DocumentUpdateResult(ok: Boolean, id: String, rev: String)

case class Document(head: DocumentHeader, body: JsObject)

case class DocumentHeader(id: String, rev: String)


object DocumentReaders {
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
}
