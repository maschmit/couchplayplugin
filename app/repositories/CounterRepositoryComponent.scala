package repositories

import concurrent.Future

import play.api.cache.Cache
import play.api.Play.current

import models._

trait CounterRepositoryComponent {
  val counterRepository: CounterRepository

  trait CounterRepository {
    def counters: Future[List[CounterWithAggregate]]
    def add(newCounter: Counter): Future[Unit]
    def increment(id: CounterId, increment: Time): Future[Unit]
  }
  
  class CacheCounterRepository extends CounterRepository {
  
	import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def counters: Future[List[CounterWithAggregate]] = Future {
    	Cache.getAs[List[Counter]]("counters")
	      .getOrElse(Nil)
	      .map { counter =>
	        new CounterWithAggregate(counter, Cache.getAs[Int](counter.name).map(Time(_)).getOrElse(NoTime)) }
	  }

	def add(newCounter: Counter) = Future {
	  	val counters = newCounter :: Cache.getAs[List[Counter]]("counters").getOrElse(Nil)
	  	Cache.set("counters", counters)
	  }

  	def increment(id: CounterId, increment: Time) = Future {
  		Cache.set(id.id, increment.minutes + Cache.getAs[Int](id.id).getOrElse(0))
  	}
  }
}
