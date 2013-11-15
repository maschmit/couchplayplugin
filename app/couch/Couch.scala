
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results

object Couch {
	def url(url: String): Couch = {
		new Couch(url)
	}

  class Couch(val url: String) {
    def removeDb(name: String): Future[Boolean] =
      WS.url(url + name).delete().map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )

	def addDb(name: String): Future[Boolean] =
      WS.url(url + name).put(Results.EmptyContent()).map( response =>
      	(response.json \ "ok").asOpt[Boolean].getOrElse(false) )
  }
}
