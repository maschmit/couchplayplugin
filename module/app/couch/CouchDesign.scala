package couch

import view._
import error.CouchErrors

import ViewReaders._
import Couch._

import scala.concurrent.Future
import play.api.libs.ws._    // could make some wrapper to avoid using ws & json but it might not be worth it
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass in


class CouchDesign(val database: CouchDatabase, val name: String) {
  def url = s"${database.url}/_design/$name"

  def view(vName: String) = new ViewQueryBuilder[ViewResult](this, vName)
}

class ViewQueryBuilder[T <: ViewResult](val design: CouchDesign, val name: String,
    val group: Option[Boolean] = None, val reduce: Option[Boolean] = None) {

  implicit def url = s"${design.url}/_view/$name$params"
  def params = paramString(List(("group", group), ("reduce", reduce)))

  def grouped = new ViewQueryBuilder[T](design, name, Some(true), reduce)
  
  /** Sets the reduce=false parameter on the view request - this should mean
    * that a Future[MapViewResult] is returned
    */
  def notReduced = new ViewQueryBuilder[MapViewResult](design, name, group, Some(false))

  /** Sets the reduce=true parameter on the view request - this should mean
    * that either a Future[ReduceViewResult] is returned or a CouchError is thrown
    */
  def reduced = new ViewQueryBuilder[ReduceViewResult](design, name, group, Some(true))

  def get(): Future[T] = 
    WS.url(url).get().map( response => response.status match {
        case 200 => response.json.as[ViewResult].asInstanceOf[T]
        case 404 => throw CouchErrors("GET", response.json).docNotFound
        case _ =>  throw CouchErrors("GET", response.json).general
      })

  private def paramString(params: Seq[(String, Option[Any])]) = "?" + params
      .collect { case (name, Some(value)) => s"$name=$value" }.mkString("&")
}
