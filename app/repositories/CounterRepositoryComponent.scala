package repositories

import concurrent.Future

import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._

import couch.Couch

import models._

trait CounterRepositoryComponent {
  val counterRepository: CounterRepository

  trait CounterRepository {
    def counters: Future[Seq[CounterWithAggregate]]
    def add(newCounter: Counter): Future[CounterId]
    def increment(id: CounterId, increment: Time): Future[Unit]
  }
  
  class CouchCounterRepository extends CounterRepository {
	import play.api.libs.json._
	import play.api.libs.functional.syntax._

	val couchDb = Couch("http://localhost:5984/").db("counters")

	implicit val counterWrites = new Writes[Counter] {
	  def writes(c: Counter): JsValue = {
	  	Json.obj(
	  		"type" -> "counter",
	  		"name" -> c.name
	  		)
	  }
	}

	implicit val incrementWrites = new Writes[CounterIncrement] {
	  def writes(c: CounterIncrement): JsValue = {
	  	Json.obj(
	  		"type" -> "increment",
	  		"counterId" -> c.counterId,
	  		"minutes" -> c.minutes
	  	  )
	  }
	}
  
	import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def counters: Future[Seq[CounterWithAggregate]] = 
      WS.url("http://localhost:5984/counters/_design/counters/_view/counters?group=true").get().map(
          response => for {
       	  row <- (response.json \ "rows").as[Seq[JsValue]]
          id <- (row \ "key").asOpt[String]
          name <- (row \ "value" \ "name").asOpt[String]
          minutes <- (row \ "value" \ "minutes").asOpt[Int]
        } yield new CounterWithAggregate(id, Counter(name), TimeCounter(minutes)))

	def add(newCounter: Counter): Future[CounterId] = 
	  couchDb.create(Json.toJson(newCounter)).map(created =>
	  	  CounterId(created.id))

  	def increment(increment: CounterIncrement) = 
  	  couchDb.create(Json.toJson(increment)).map(r => ())

  	def increment(id: CounterId, time: Time) = 
  	  increment(CounterIncrement(id.id, time.minutes))
  }
}
