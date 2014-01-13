package couch.sync

import couch.DocumentPointer
import couch.Couch.CouchDatabase
import couch.document.DocumentHeader

import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in

import java.io.File


object CouchSync {
  /** Sync from a json value */
  def apply(json: JsValue): CouchDocumentSync = new JsonCouchDocumentSync(json)

  /** Sync an entire directory */
  def apply(dir: File): CouchDocumentSetSync = new DirectoryCouchDocumentSetSync(None, dir)

  class JsonCouchDocumentSync(json:JsValue) extends CouchDocumentSync {
  	def to(ptr: DocumentPointer): Future[DocumentHeader] =
  	  ptr.getOpt().flatMap {
  	  	case None => ptr.create(json)
  	  	case Some(doc) if doc.body.equals(json) => Future.successful(doc.head)
  	  	case Some(doc) => ptr.rev(doc.head.rev).replace(json)
  	  }
  }

  class DirectoryCouchDocumentSetSync(root: Option[String], dir: File) extends CouchDocumentSetSync {
  	def to(db: CouchDatabase) = 
  	  dir.listFiles.flatMap {
  	  	case file if file.isFile && file.getName().endsWith(".json") =>
  	  	  Seq(new JsonCouchDocumentSync(getJson(file)).to(db.doc(jsonFileName(file.getName))))
  	  	case subdir if subdir.isDirectory =>
          new DirectoryCouchDocumentSetSync(Some(subRoot(subdir.getName)), subdir).to(db)
  	  }

    private def jsonFileName(name: String) = root.map(_ + "/").getOrElse("") + name.substring(0,name.length-5)
    private def subRoot(name: String) = root.map(_ + "/").getOrElse("") + name
  }
	
  private def getJson(file: File): JsObject =
    Json.parse(io.Source.fromFile(file).mkString).as[JsObject]
}

trait CouchDocumentSync {
  def to(ptr: DocumentPointer): Future[DocumentHeader]
}

trait CouchDocumentSetSync {
  def to(db: CouchDatabase): Seq[Future[Any]]
}
