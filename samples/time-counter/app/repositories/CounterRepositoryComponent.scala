package repositories

import concurrent.Future

import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._

import couch.Couch
import couch.view.ReduceViewElement

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

	val couchDb = Couch.load.db("default")

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
      couchDb.design("counters").view("counters").grouped.get().map(_.rows.map {
      	case ReduceViewElement(key, value) => for {
      	  k <- key.asOpt[String]
      	  name <- (value \ "name").asOpt[String]
      	  minutes <- (value \ "minutes").asOpt[Int]
      	} yield new CounterWithAggregate(k, Counter(name), TimeCounter(minutes))
      }.collect { case Some(v) => v })

	def add(newCounter: Counter): Future[CounterId] = 
	  couchDb.create(Json.toJson(newCounter)).map(created =>
	  	  CounterId(created.id))

  	def increment(increment: CounterIncrement) = 
  	  couchDb.create(Json.toJson(increment)).map(r => ())

  	def increment(id: CounterId, time: Time) = 
  	  increment(CounterIncrement(id.id, time.minutes))
  }
}
