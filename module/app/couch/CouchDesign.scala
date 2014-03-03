package couch

import view._
import error.CouchErrors

import ViewReaders._
import Couch._

import scala.concurrent.Future
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in


class CouchDesign(private val designRequestGen: RequestGenerator) {
  lazy val url = designRequestGen(Nil).url

  def view(name: String) = new ViewQueryBuilder[ViewResult](designRequestGen("_view" :: name :: Nil))
}

class ViewQueryBuilder[T <: ViewResult](val designRequest: WS.WSRequestHolder,
    val group: Option[Boolean] = None, val reduce: Option[Boolean] = None) {

  implicit def url: String = request.url
      
  def request = designRequest.withQueryString(Seq(("group", group), ("reduce", reduce))
    .collect { case (name, Some(value)) => (name, value.toString) }: _*)

  def grouped = new ViewQueryBuilder[T](designRequest, Some(true), reduce)
  
  /** Sets the reduce=false parameter on the view request - this should mean
    * that a Future[MapViewResult] is returned
    */
  def notReduced = new ViewQueryBuilder[MapViewResult](designRequest, group, Some(false))

  /** Sets the reduce=true parameter on the view request - this should mean
    * that either a Future[ReduceViewResult] is returned or a CouchError is thrown
    */
  def reduced = new ViewQueryBuilder[ReduceViewResult](designRequest, group, Some(true))

  def get(): Future[T] = 
    request.get().map( response => response.status match {
        case 200 => response.json.as[ViewResult].asInstanceOf[T]
        case 404 => throw CouchErrors("GET", response.json).docNotFound
        case _ =>  throw CouchErrors("GET", response.json).general
      })
}
