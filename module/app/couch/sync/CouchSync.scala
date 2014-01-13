package couch.sync

import couch.DocumentPointer

import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in

object CouchSync {
  def apply(json: JsValue): CouchDocumentSync = new JsonCouchDocumentSync(json)

  class JsonCouchDocumentSync(json:JsValue) extends CouchDocumentSync {
  	def to(ptr: DocumentPointer): Future[Any] =
  	  ptr.getOpt().map {
  	  	case None => ptr.create(json)
  	  	case Some(doc) if doc.body.equals(json) => doc.head
  	  	case Some(doc) => ptr.rev(doc.head.rev).replace(json)
  	  }
  	  
  }
}

trait CouchDocumentSync {
  def to(ptr: DocumentPointer): Future[Any]
}
