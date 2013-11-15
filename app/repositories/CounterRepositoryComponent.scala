package repositories

import concurrent.Future

import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws._

import models._

trait CounterRepositoryComponent {
  val counterRepository: CounterRepository

  trait CounterRepository {
    def counters: Future[Seq[CounterWithAggregate]]
    def add(newCounter: Counter): Future[CounterId]
    def increment(id: CounterId, increment: Time): Future[Unit]
  }
  
  class CacheCounterRepository extends CounterRepository {
  
	import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def counters: Future[Seq[CounterWithAggregate]] = Future {
    	Cache.getAs[List[Counter]]("counters")
	      .getOrElse(Nil)
	      .map { counter =>
	        new CounterWithAggregate(counter.name, counter, Cache.getAs[Int](counter.name).map(Time(_)).getOrElse(NoTime)) }
	  }

	def add(newCounter: Counter): Future[CounterId] = Future {
	  	val counters = newCounter :: Cache.getAs[List[Counter]]("counters").getOrElse(Nil)
	  	Cache.set("counters", counters)
	  	CounterId(newCounter.name)
	  }

  	def increment(id: CounterId, increment: Time) = Future {
  		Cache.set(id.id, increment.minutes + Cache.getAs[Int](id.id).getOrElse(0))
  	}
  }
  
  class CouchCounterRepository extends CounterRepository {
	import play.api.libs.json._
	import play.api.libs.functional.syntax._

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
    	WS.url("http://localhost:5984/counters/").post(Json.toJson(newCounter)).map(
    		response => (response.json \ "id").as[String] ).map(CounterId.apply)

  	def increment(id: CounterId, increment: Time) = 
  	  WS.url("http://localhost:5984/counters/").post(Json.toJson(CounterIncrement(id.id, increment.minutes))).map(r => ())
  }
}
