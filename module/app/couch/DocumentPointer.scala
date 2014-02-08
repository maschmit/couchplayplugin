package couch

import document._
import error._

import ErrorReaders._

import scala.concurrent.Future
import play.api.Play.current // TODO : implicitly pass in
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import WS.WSRequestHolder
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in


object DocumentPointer {
  def apply(request: WSRequestHolder) = new DocumentPointer(request)
  def apply(request: WSRequestHolder, rev: String) = new DocumentRevisionPointer(request)
}

class DocumentPointer(private val request: WSRequestHolder) {
  import DocumentReaders._
  
  def url: String = request.url

  def create[T](body: JsValue): Future[DocumentUpdateResult] = 
    request.put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw DocumentCreationFailed(response.json.as[CouchErrorInfo])
      })

  def get(): Future[Document] = 
    request.get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw DocumentNotFound(response.json.as[CouchErrorInfo])
        case _ =>  throw GeneralCouchError(response.json.as[CouchErrorInfo])
      })

  def getOpt(): Future[Option[Document]] = 
    request.get().map( response => response.status match {
        case 200 => Some(response.json.as[Document])
        case 404 => None
        case _ =>  throw GeneralCouchError(response.json.as[CouchErrorInfo])
      })

  def rev(rev: String): DocumentRevisionPointer = new DocumentRevisionPointer(request.withQueryString(("rev", rev)))
}

class DocumentRevisionPointer(private val request: WSRequestHolder) {
  import DocumentReaders._

  def url: String = request.url

  def replace[T](body: JsValue): Future[DocumentUpdateResult] = 
    request.put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw DocumentCreationFailed(response.json.as[CouchErrorInfo])
      })

  def delete() =
    request.delete().map( response => response.status match {
        case 200 => response.json
        case 404 => throw DocumentNotFound(response.json.as[CouchErrorInfo])
        case _ =>  throw GeneralCouchError(response.json.as[CouchErrorInfo])
      })

  def get(): Future[Document] = 
    request.get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw DocumentNotFound(response.json.as[CouchErrorInfo])
        case _ =>  throw GeneralCouchError(response.json.as[CouchErrorInfo])
      })
}
