package couch.error

import play.api.libs.json._
import play.api.libs.functional.syntax._


abstract class CouchError extends Exception {
  protected def info: CouchErrorInfo
  protected def method: String
  protected def url: String
  def error: String = info.error
  def reason: String = info.reason
  override def getMessage: String = s"Error: '$error', Reason: '$reason', Request: $method to $url"
}

case class DatabaseNotFound(override val method: String, override val url: String, override val info: CouchErrorInfo) extends CouchError
case class DocumentNotFound(override val method: String, override val url: String, override val info: CouchErrorInfo) extends CouchError
case class DocumentCreationFailed(override val method: String, override val url: String, override val info: CouchErrorInfo) extends CouchError
case class GeneralCouchError(override val method: String, override val url: String, override val info: CouchErrorInfo) extends CouchError

case class CouchErrors(method: String, json: JsValue)(implicit url: String) {
  import ErrorReaders.errorinfoReads
  private def info = json.as[CouchErrorInfo]
  def docCreationFailed = DocumentCreationFailed(method, url, info)
  def docNotFound = DocumentNotFound(method, url, info)
  def dbNotFound = DatabaseNotFound(method, url, info)
  def general = GeneralCouchError(method, url, info)
}

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
