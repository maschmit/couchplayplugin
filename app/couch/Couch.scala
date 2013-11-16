
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results

object Couch {
	def url(url: String): Couch = {
		new Couch(url)
	}

  class Couch(val url: String) {
    def removeDb(name: String): Future[Boolean] =
      WS.url(url + name).delete().map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

	def addDb(name: String): Future[Boolean] =
      WS.url(url + name).put(Results.EmptyContent()).map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

    def db(name: String): CouchDatabase =
      new CouchDatabase(this, name)
  }

  class CouchDatabase(val couch: Couch, val name: String) {
  	private def url = couch.url + name

  	def create(body: JsObject): Future[DocumentUpdateResponse] = 
  	  WS.url(url).post(body).map( response => response.status match {
          case 201 => SuccessfulUpdateResponse(DocumentHeader((response.json \ "id").as[String], (response.json \ "rev").as[String]))
          case _ => FailedUpdateResponse(CouchError((response.json \ "error").as[String], (response.json \ "reason").as[String]))
        })

  	def create[T](id: String, body: T): DocumentUpdateResponse = ???
  	def replace[T](head: DocumentHeader, body: T): DocumentUpdateResponse = ???
  	def delete(id: String) = ???

  	def get(id: String): Future[DocumentRequestResponse] = 
  	  WS.url(url + '/' + id).get().map( response => response.status match {
          case 200 => Document(DocumentHeader((response.json \ "_id").as[String], (response.json \ "_rev").as[String]), response.json.asInstanceOf[JsObject])
          case 404 => NoDocument(CouchError((response.json \ "error").as[String], (response.json \ "reason").as[String]))
        })
  }
}

abstract class DocumentRequestResponse
case class Document(head: DocumentHeader, body: JsObject) extends DocumentRequestResponse
case class NoDocument(error: CouchError) extends DocumentRequestResponse

case class DocumentHeader(id: String, rev: String)
case class CouchError(error: String, reason: String)

abstract class DocumentUpdateResponse
case class SuccessfulUpdateResponse(head: DocumentHeader) extends DocumentUpdateResponse
case class FailedUpdateResponse(error: CouchError) extends DocumentUpdateResponse {
	
}
