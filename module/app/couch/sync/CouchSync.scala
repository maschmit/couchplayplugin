package couch.sync

import couch.DocumentPointer
import couch.Couch.CouchDatabase
import couch.document._

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
  	def to(ptr: DocumentPointer): Future[DocumentHeader] = check(ptr).flatMap(_.run())

    def check(ptr: DocumentPointer): Future[SingleMatchResult] = 
      ptr.getOpt().map {
        case None => DocumentNoMatch(sourceName, json, ptr, None)
        case Some(doc) if doc.body.equals(json) => DocumentMatch(sourceName, ptr)
        case Some(doc) => DocumentNoMatch(sourceName, json, ptr, Some(doc))
      }
  }

  class FileCouchDocumentSync(file: File) extends JsonCouchDocumentSync(getJson(file)) {
    override def sourceName = file.getPath
  }

  class DirectoryCouchDocumentSetSync(root: Option[String], dir: File) extends CouchDocumentSetSync {
  	def to(db: CouchDatabase) = check(db).flatMap(_.run())

    def check(db: CouchDatabase) = MatchResult.all(checkSeperate(db))

    private def checkSeperate(db: CouchDatabase): Seq[Future[SingleMatchResult]] = 
      dir.listFiles.flatMap {
        case file if file.isFile && file.getName().endsWith(".json") =>
          Seq(new FileCouchDocumentSync(file).check(db.doc(jsonFileName(file.getName))))
        case subdir if subdir.isDirectory =>
          new DirectoryCouchDocumentSetSync(Some(subRoot(subdir.getName)), subdir).checkSeperate(db)
      }

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
  def to(db: CouchDatabase): Future[Seq[DocumentHeader]]
  def check(db: CouchDatabase): Future[MultiMatchResult]
}

object MatchResult {

  private class CombinedMatchResult[T <: SingleMatchResult](s: Seq[T], matches: Boolean) extends collection.SeqProxy[T] with MultiMatchResult {
    def isMatch = matches
    def self = s
    override def toString = s"CombinedMatchResult : $isMatch {$self}"
    def run(): Future[Seq[DocumentHeader]] = {
      val runningUpdates = s.collect { case r: SingleMatchResult if !r.isMatch => r.run() }
      Future.fold(runningUpdates)(List[DocumentHeader]())(_.::(_)).map(_.toSeq)
    }
  }

  def all(fs: Seq[Future[SingleMatchResult]]): Future[MultiMatchResult] = 
    Future.fold(fs)((List[SingleMatchResult](), true)) {
      case ((list, matches), b) => (b :: list, matches && b.isMatch)
    } .map {
      case (l, m) => new CombinedMatchResult(l, m)
    }
}


trait MatchResult {
  def isMatch: Boolean
  def run(): Future[Any]
}
trait MultiMatchResult extends Seq[SingleMatchResult] with MatchResult {
  def run(): Future[Seq[DocumentHeader]]
}
trait SingleMatchResult extends MatchResult {
  def run(): Future[DocumentHeader]
  def destination: DocumentPointer
}
case class DocumentMatch(source: String, destination: DocumentPointer) extends SingleMatchResult {
  def isMatch = true
  def run(): Future[DocumentHeader] = Future.successful(null)
}
case class DocumentNoMatch(source: String, sourceJson: JsValue, destination: DocumentPointer, destinationJson: Option[Document]) extends SingleMatchResult {
  def isMatch = false
  def run(): Future[DocumentHeader] = destinationJson match {
    case None => destination.create(sourceJson)
    case Some(doc) => destination.rev(doc.head.rev).replace(sourceJson)
  }
}
