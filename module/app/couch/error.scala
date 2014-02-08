package couch.error

import play.api.libs.json._
import play.api.libs.functional.syntax._


abstract class CouchError extends Exception {
  protected def info: CouchErrorInfo
  def error: String = info.error
  def reason: String = info.reason
  override def getMessage: String = s"Error: '$error', Reason: '$reason'"
}

case class DatabaseNotFound(override val info: CouchErrorInfo) extends CouchError
case class DocumentNotFound(override val info: CouchErrorInfo) extends CouchError
case class DocumentCreationFailed(override val info: CouchErrorInfo) extends CouchError
case class GeneralCouchError(override val info: CouchErrorInfo) extends CouchError

case class CouchErrorInfo(error: String, reason: String)

class ClientImplementationException(message: String) extends
  Exception(s"The response was not understood by the client - it may be that this is a version which has not been tested with the client ($message)")

object ErrorReaders {
  private def couchErrorReads = (
  	(__ \ "error").read[String] ~
  	(__ \ "reason").read[String]
  )

  implicit val errorinfoReads: Reads[CouchErrorInfo] =
    couchErrorReads(CouchErrorInfo)
}
