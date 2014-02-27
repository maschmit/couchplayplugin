package couch

import document._
import error.CouchErrors

import scala.concurrent.{Future,ExecutionContext}
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import WS.WSRequestHolder


object DocumentPointer {
  def apply(request: WSRequestHolder) = new DocumentPointer(request)
  def apply(request: WSRequestHolder, rev: String) = new DocumentRevisionPointer(request)
}

class DocumentPointer(private val request: WSRequestHolder) {
  import DocumentReaders._
  
  implicit def url: String = request.url

  def create[T](body: JsValue)(implicit executor: ExecutionContext): Future[DocumentUpdateResult] = 
    request.put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw CouchErrors("PUT", response.json).docCreationFailed
      })

  def get()(implicit executor: ExecutionContext): Future[Document] = 
    request.get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw CouchErrors("GET", response.json).docNotFound
        case _ =>  throw CouchErrors("GET", response.json).general
      })

  def getOpt()(implicit executor: ExecutionContext): Future[Option[Document]] = 
    request.get().map( response => response.status match {
        case 200 => Some(response.json.as[Document])
        case 404 => None
        case _ =>  throw CouchErrors("GET", response.json).general
      })

  def rev(rev: String): DocumentRevisionPointer = new DocumentRevisionPointer(request.withQueryString(("rev", rev)))
}

class DocumentRevisionPointer(private val request: WSRequestHolder) {
  import DocumentReaders._

  implicit def url: String = request.url

  def replace[T](body: JsValue)(implicit executor: ExecutionContext): Future[DocumentUpdateResult] = 
    request.put(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw CouchErrors("PUT", response.json).docCreationFailed
      })

  def delete()(implicit executor: ExecutionContext) =
    request.delete().map( response => response.status match {
        case 200 => response.json
        case 404 => throw CouchErrors("DELETE", response.json).docNotFound
        case _ =>  throw CouchErrors("DELETE", response.json).general
      })

  def get()(implicit executor: ExecutionContext): Future[Document] = 
    request.get().map( response => response.status match {
        case 200 => response.json.as[Document]
        case 404 => throw CouchErrors("GET", response.json).docNotFound
        case _ =>  throw CouchErrors("GET", response.json).general
      })
}
