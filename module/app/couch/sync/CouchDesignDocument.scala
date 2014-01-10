package couch.sync

import play.api.libs.json._


case class CouchDesignDocument(val json: JsObject)

object CouchDesignDocument {
	def read(path: String): CouchDesignDocument =
	  CouchDesignDocument(getJson(path))
	
	private def getJson(path: String): JsObject =
	  Json.parse(io.Source.fromFile(path).mkString).as[JsObject]
}
