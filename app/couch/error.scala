package couch.error

import play.api.libs.json._
import play.api.libs.functional.syntax._


abstract class CouchError extends Exception {
	def error: String
	def reason: String
	override def getMessage: String = s"Error: '$error', Reason: '$reason'"
}
case class DocumentNotFound(override val error: String, override val reason: String) extends CouchError
case class DocumentCreationFailed(override val error: String, override val reason: String) extends CouchError
case class GeneralCouchError(override val error: String, override val reason: String) extends CouchError


object ErrorReaders {
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
