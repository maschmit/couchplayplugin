package couch

import document._
import error.CouchErrors
import config.CouchConfiguration

import scala.concurrent.Future
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in
import play.api.mvc.Results // TODO : don't use this


trait CouchHost {
  def url: String
  def removeDb(name: String): Future[Boolean]
  def addDb(name: String): Future[Boolean]
  def db(name: String): CouchDatabase
  def user(user: String, password: String): CouchHost
}

trait CouchDatabase{
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

/** Api entry-point for building requests on a CouchDB instance */
object Couch {
  import DocumentReaders._

  /** Specify a host to start building a request
    * 
    * {{{
    * val couch = Couch.host("http://localhost:5984/"))
    * }}}
    */
  def host(url: String): CouchHost = {
    def stripSlashes(strings: List[String]) =
      strings.map(_.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse)
    def makePath(path: List[String]) =
      stripSlashes(url :: path).mkString("/")
    new BasicCouchHost((path: List[String]) => WS.url(makePath(path)))
  }

  /** Load a database specified in config
    * 
    * ie. if in conf/application.conf you have:
    * {{{
    * couch.db.default.host="http://localhost:5984/"
    * couch.db.default.database="counters"
    * couch.db.default.dir="conf/couch"
    * couch.db.default.username="admin"
    * couch.db.default.password="admin"
    * }}}
    * you can start building requests with:
    * {{{
    * val db = Couch.load.db("default")
    * db.get("docId")
    * }}}
    */
  lazy val load = new CouchConfigDBLoader(CouchConfiguration.config)

  type RequestGenerator = (List[String]) => WS.WSRequestHolder

  class BasicCouchHost(private val hostRequest: RequestGenerator) extends CouchHost {
    private def dbRequest(name: String) = hostRequest(List(name))

    implicit def url = hostRequest(Nil).url

    def removeDb(name: String): Future[Boolean] =
      dbRequest(name).delete().map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

	  def addDb(name: String): Future[Boolean] =
      dbRequest(name).put(Results.EmptyContent()).map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

    def db(name: String): CouchDatabase =
      new BasicCouchDatabase((path: List[String]) => hostRequest(name :: path))

    def user(user: String, password: String) =
      new BasicCouchHost(hostRequest(_).withAuth(user, password, com.ning.http.client.Realm.AuthScheme.BASIC))
  }

  class BasicCouchDatabase(private val dbRequestGen: RequestGenerator) extends CouchDatabase {
  	implicit def url = dbRequest.url
    private def dbRequest = dbRequestGen(Nil)

  	def create(body: JsValue): Future[DocumentUpdateResult] = 
  	  dbRequest.post(body.as[JsObject]).map( response => response.status match {
        case 201 => response.json.as[DocumentUpdateResult]
        case _ => throw CouchErrors("POST", response.json).docCreationFailed
      })

  	def create[T](id: String, body: JsValue): Future[DocumentUpdateResult] = 
      doc(id).create(body)

    def replace[T](head: DocumentHeader, body: JsValue): Future[DocumentUpdateResult] = 
      doc(head.id).rev(head.rev).replace(body)

    def delete(docHead: DocumentHeader) =
      doc(docHead.id).rev(docHead.rev).delete()

  	def get(id: String): Future[Document] = 
  	  doc(id).get()

    def doc(id: String): DocumentPointer =
      new DocumentPointer(dbRequestGen(List(id)))

    def design(ddName: String) =
      new CouchDesign(this, ddName)

    def info(): Future[DatabaseInfo] = 
      dbRequest.get().map( response => response.status match {
        case 200 => DatabaseInfo()
        case 404 => throw CouchErrors("GET", response.json).dbNotFound
        case _ => throw CouchErrors("GET", response.json).general
      })
  }
}
