package couch

import document._
import view._
import error._

import scala.concurrent.Future
import play.api.Play.current // TODO : implicitly pass in
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in
import play.api.mvc.Results // TODO : don't use this

object Couch {
  import ErrorReaders._
  import DocumentReaders._
  import ViewReaders._

  def apply(url: String): Couch = {
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
  	def url = couch.url + name

  	def create(body: JsValue): Future[DocumentUpdateResult] = 
  	  WS.url(url).post(body.as[JsObject]).map( response => response.status match {
          case 201 => response.json.as[DocumentUpdateResult]
          case _ => throw response.json.as[DocumentCreationFailed]
        })

  	def create[T](id: String, body: JsValue): Future[DocumentUpdateResult] = 
      WS.url(url + s"/$id").put(body.as[JsObject]).map( response => response.status match {
          case 201 => response.json.as[DocumentUpdateResult]
          case _ => throw response.json.as[DocumentCreationFailed]
        })

  	def replace[T](head: DocumentHeader, body: T): Future[DocumentUpdateResult] = ???
  	def delete(id: String) = ???

  	def get(id: String): Future[Document] = 
  	  WS.url(url + '/' + id).get().map( response => response.status match {
          case 200 => response.json.as[Document]
          case 404 => throw response.json.as[DocumentNotFound]
          case _ =>  throw response.json.as[GeneralCouchError]
        })

    def design(ddName: String) = new CouchDesign(this, ddName)
  }

  class CouchDesign(val database: CouchDatabase, val name: String) {
    def url = s"${database.url}/_design/$name"

    def view(vName: String) = new ViewQueryBuilder(this, vName, None)
  }

  class ViewQueryBuilder(val design: CouchDesign, val name: String, val group: Option[Boolean]) {
    def url = s"${design.url}/_view/$name" + params
    def params = paramString(List(("group", group)))
    def grouped = new ViewQueryBuilder(design, name, Some(true))
    def get(): Future[ViewResult] = 
      WS.url(url).get().map( response => response.status match {
          case 200 => response.json.as[ViewResult]
          case 404 => throw response.json.as[DocumentNotFound]
          case _ =>  throw response.json.as[GeneralCouchError]
        })
  }

  private def paramString(params: Seq[(String, Option[Any])]) = "?" + params
      .filterNot(_._2.isEmpty).map { case (name, Some(value)) => s"$name=$value" }.mkString("&")
}
