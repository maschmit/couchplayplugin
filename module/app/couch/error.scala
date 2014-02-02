package couch.error

import play.api.libs.json._
import play.api.libs.functional.syntax._


abstract class CouchError extends Exception {
	def error: String
	def reason: String
	override def getMessage: String = s"Error: '$error', Reason: '$reason'"
}
case class DatabaseNotFound(override val error: String, override val reason: String) extends CouchError
case class DocumentNotFound(override val error: String, override val reason: String) extends CouchError
case class DocumentCreationFailed(override val error: String, override val reason: String) extends CouchError
case class GeneralCouchError(override val error: String, override val reason: String) extends CouchError

class ClientImplementationException(message: String) extends
  Exception(s"The response was not understood by the client - it may be that this is a version which has not been tested with the client ($message)")

object ErrorReaders {
  private def couchErrorReads = (
  	(__ \ "error").read[String] ~
  	(__ \ "reason").read[String]
  )

  implicit val dbNotFoundReads: Reads[DatabaseNotFound] =
    couchErrorReads(DatabaseNotFound)

  implicit val docNotFoundReads: Reads[DocumentNotFound] =
    couchErrorReads(DocumentNotFound)

  implicit val docCreationFailedReads: Reads[DocumentCreationFailed] =
    couchErrorReads(DocumentCreationFailed)

  implicit val generalErrorReads: Reads[GeneralCouchError] =
    couchErrorReads(GeneralCouchError)
}
