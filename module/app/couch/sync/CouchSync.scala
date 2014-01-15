package couch.sync

import couch.DocumentPointer
import couch.Couch.CouchDatabase
import couch.document.DocumentHeader

import play.api.libs.json._
import scala.concurrent.Future
import scala.async.Async.{async, await}
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in

import java.io.File


object CouchSync {
  /** Sync from a json value */
  def apply(json: JsValue): CouchDocumentSync = new JsonCouchDocumentSync(json)

  /** Sync an entire directory */
  def apply(dir: File): CouchDocumentSetSync = new DirectoryCouchDocumentSetSync(None, dir)

  class JsonCouchDocumentSync(json:JsValue) extends CouchDocumentSync {
    def sourceName = "(In memory json value)"
  	def to(ptr: DocumentPointer): Future[DocumentHeader] =
  	  ptr.getOpt().flatMap {
  	  	case None => ptr.create(json)
  	  	case Some(doc) if doc.body.equals(json) => Future.successful(doc.head)
  	  	case Some(doc) => ptr.rev(doc.head.rev).replace(json)
  	  }

    def check(ptr: DocumentPointer): Future[MatchResult] = 
      ptr.getOpt().map {
        case None => DocumentNoMatch(sourceName, json, ptr, None)
        case Some(doc) if doc.body.equals(json) => DocumentMatch(sourceName, ptr)
        case Some(doc) => DocumentNoMatch(sourceName, json, ptr, Some(doc.body))
      }
  }

  class FileCouchDocumentSync(file: File) extends JsonCouchDocumentSync(getJson(file)) {
    override def sourceName = file.getPath
  }

  class DirectoryCouchDocumentSetSync(root: Option[String], dir: File) extends CouchDocumentSetSync {
  	def to(db: CouchDatabase) = 
  	  dir.listFiles.flatMap {
  	  	case file if file.isFile && file.getName().endsWith(".json") =>
  	  	  Seq(new JsonCouchDocumentSync(getJson(file)).to(db.doc(jsonFileName(file.getName))))
  	  	case subdir if subdir.isDirectory =>
          new DirectoryCouchDocumentSetSync(Some(subRoot(subdir.getName)), subdir).to(db)
  	  }

    def check(db: CouchDatabase): Future[MatchResult] = 
      MatchResult.all(dir.listFiles.map {
        case file if file.isFile && file.getName().endsWith(".json") =>
          new JsonCouchDocumentSync(getJson(file)).check(db.doc(jsonFileName(file.getName)))
        case subdir if subdir.isDirectory =>
          new DirectoryCouchDocumentSetSync(Some(subRoot(subdir.getName)), subdir).check(db)
      })

    private def jsonFileName(name: String) = root.map(_ + "/").getOrElse("") + name.substring(0,name.length-5)
    private def subRoot(name: String) = root.map(_ + "/").getOrElse("") + name
  }
	
  private def getJson(file: File): JsObject =
    Json.parse(io.Source.fromFile(file).mkString).as[JsObject]
}

trait CouchDocumentSync {
  def to(ptr: DocumentPointer): Future[DocumentHeader]
  def check(ptr: DocumentPointer): Future[MatchResult]
}

trait CouchDocumentSetSync {
  def to(db: CouchDatabase): Seq[Future[Any]]
  def check(db: CouchDatabase): Future[MatchResult]
}

object MatchResult {
  def all[T <: MatchResult](fs: Seq[Future[T]]): Future[Seq[T] with MatchResult] = async {
    val b = collection.mutable.ListBuffer[T]()
    var _fs = fs.toList
    var matches = true
    while(!_fs.isEmpty) {
      val next = await { _fs.head }
      matches = matches && next.isMatch
      b.append(next)
      _fs = _fs.tail
    }
    class CombinedMatchResult[T <: MatchResult](s: Seq[T]) extends collection.SeqProxy[T] with Seq[T] with MatchResult {
      def isMatch = matches
      def self = s
    }
    new CombinedMatchResult(b)
  }
}


trait MatchResult {
  def isMatch: Boolean
}
case class DocumentMatch(source: String, destination: DocumentPointer) extends MatchResult {
  def isMatch = true
}
case class DocumentNoMatch(source: String, sourceJson: JsValue, destination: DocumentPointer, destinationJson: Option[JsValue]) extends MatchResult {
  def isMatch = false
}
