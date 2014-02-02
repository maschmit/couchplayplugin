package couch

import document._
import error._

import scala.concurrent.Future
import play.api.Play.current // TODO : implicitly pass in
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in
import play.api.mvc.Results // TODO : don't use this



trait Couch {
  def url: String
  def removeDb(name: String): Future[Boolean]
  def addDb(name: String): Future[Boolean]
  def db(name: String): CouchDatabase
}

trait CouchDatabase{
  def name: String
  def url: String
  def create(body: JsValue): Future[DocumentUpdateResult]
  def create[T](id: String, body: JsValue): Future[DocumentUpdateResult]
  def replace[T](head: DocumentHeader, body: JsValue): Future[DocumentUpdateResult]
  def delete(docHead: DocumentHeader): Future[JsValue]
  def get(id: String): Future[Document]
  def doc(id: String): DocumentPointer
  def design(ddName: String): CouchDesign
  def info(): Future[DatabaseInfo]
}

object Couch {
  import ErrorReaders._
  import DocumentReaders._

  def apply(url: String): Couch = {
    new BasicCouch(url)
  }

  class BasicCouch(val url: String) extends Couch {
    def removeDb(name: String): Future[Boolean] =
      WS.url(url + name).delete().map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

	  def addDb(name: String): Future[Boolean] =
      WS.url(url + name).put(Results.EmptyContent()).map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

    def db(name: String): CouchDatabase =
      new BasicCouchDatabase(this, name)
  }

  class BasicCouchDatabase(val couch: Couch, val name: String) extends CouchDatabase {
  	def url = couch.url + name
    private def docUrl(id: String): String = s"$url/$id"
    private def revUrl(docHead: DocumentHeader): String = s"$url/${docHead.id}?rev=${docHead.rev}"

  	def create(body: JsValue): Future[DocumentUpdateResult] = 
  	  WS.url(url).post(body.as[JsObject]).map( response => response.status match {
          case 201 => response.json.as[DocumentUpdateResult]
          case _ => throw response.json.as[DocumentCreationFailed]
        })

  	def create[T](id: String, body: JsValue): Future[DocumentUpdateResult] = 
      doc(id).create(body)

    def replace[T](head: DocumentHeader, body: JsValue): Future[DocumentUpdateResult] = 
      doc(head.id).rev(head.rev).replace(body)

    def delete(docHead: DocumentHeader) =
      doc(docHead.id).rev(docHead.rev).delete()

  	def get(id: String): Future[Document] = 
  	  doc(id).get()

    def doc(id: String): DocumentPointer = DocumentPointer(this, id)

    def design(ddName: String) = new CouchDesign(this, ddName)

    def info(): Future[DatabaseInfo] = 
      WS.url(url).get().map( response => response.status match {
          case 200 => DatabaseInfo()
          case 404 => throw response.json.as[DatabaseNotFound]
          case _ => throw response.json.as[GeneralCouchError]
        })
  }
}
