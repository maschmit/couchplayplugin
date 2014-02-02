package couch.document

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class DocumentUpdateResult(ok: Boolean, id: String, rev: String) extends DocumentHeader {
  lazy val head = DocumentHeaderImpl(id, rev)
}

case class Document(head: DocumentHeader, json: JsObject) {
  lazy val body = json - "_id" - "_rev"
}

case class DocumentHeaderImpl(id: String, rev: String) extends DocumentHeader

trait DocumentHeader {
  def id: String
  def rev: String
}

case class DatabaseInfo()


object DocumentReaders {
  implicit val documentCreateResult: Reads[DocumentUpdateResult] = (
  	(__ \ "ok").read[Boolean] ~
    (__ \ "id").read[String] ~
    (__ \ "rev").read[String]
    )(DocumentUpdateResult)

  implicit val documentHeader: Reads[DocumentHeader] = (
    (__ \ "_id").read[String] ~
    (__ \ "_rev").read[String]
    )(DocumentHeaderImpl)

  implicit val document: Reads[Document] = (
    documentHeader ~
    (__).read[JsObject]
  )(Document)
}
