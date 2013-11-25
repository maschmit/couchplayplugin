
import scala.concurrent.Future
import play.api.Play.current // TODO : implicitly pass in
import play.api.libs.json.JsObject
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in
import play.api.mvc.Results // TODO : don't use this

object Couch {
  import ImplicitReaders._

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

  	def create(body: JsObject): Future[DocumentUpdateResult] = 
  	  WS.url(url).post(body).map( response => response.status match {
          case 201 => response.json.as[DocumentUpdateResult]
          case _ => throw response.json.as[DocumentCreationFailed]
        })

  	def create[T](id: String, body: T): Future[DocumentUpdateResult] = ???
  	def replace[T](head: DocumentHeader, body: T): Future[DocumentUpdateResult] = ???
  	def delete(id: String) = ???

  	def get(id: String): Future[Document] = 
  	  WS.url(url + '/' + id).get().map( response => response.status match {
          case 200 => response.json.as[Document]
          case 404 => throw response.json.as[DocumentNotFound]
        })
  }
}
