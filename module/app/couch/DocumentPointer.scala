package couch

import document._
import error._

import ErrorReaders._
import Couch._

import scala.concurrent.Future
import play.api.Play.current // TODO : implicitly pass in
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in


object DocumentPointer {
  def apply(db: CouchDatabase, id: String) = new DocumentPointer(db, id)
  def apply(db: CouchDatabase, id: String, rev: String) = new DocumentRevisionPointer(db, id, rev)
}

class DocumentPointer(val db: CouchDatabase, val id: String) {
  import DocumentReaders._
  
  def url: String = s"${db.url}/$id"

  def create[T](body: JsValue): Future[DocumentUpdateResult] = 
    WS.url(url).put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw response.json.as[DocumentCreationFailed]
      })

  def get(): Future[Document] = 
    WS.url(url).get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw response.json.as[DocumentNotFound]
        case _ =>  throw response.json.as[GeneralCouchError]
      })

  def rev(rev: String): DocumentRevisionPointer = DocumentPointer(db, id, rev)
}

class DocumentRevisionPointer(val db: CouchDatabase, val id: String, val rev: String) {
  import DocumentReaders._

  def url: String = s"${db.url}/$id?rev=$rev"

  def replace[T](body: JsValue): Future[DocumentUpdateResult] = 
    WS.url(url).put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw response.json.as[DocumentCreationFailed]
      })

  def delete() =
    WS.url(url).delete().map( response => response.status match {
        case 200 => response.json
        case 404 => throw response.json.as[DocumentNotFound]
        case _ =>  throw response.json.as[GeneralCouchError]
      })

  def get(): Future[Document] = 
    WS.url(url).get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw response.json.as[DocumentNotFound]
        case _ =>  throw response.json.as[GeneralCouchError]
      })
}
