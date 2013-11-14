package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.Play.current

import models._
import repositories.CounterRepositoryComponent

trait CountersControllerTrait extends Controller {
  this: CounterRepositoryComponent =>

  def index = Action {
  	val counters = Cache.getAs[List[Counter]]("counters")
      .getOrElse(Nil)
      .map { counter => new CounterWithAggregate(counter, Cache.getAs[Int](counter.name).map(Time(_)).getOrElse(NoTime)) }
  	Ok(views.html.counters(counterForm, incrementCounterForm, counters)) 
  }

  def addCounter = Action { implicit request =>
  	val newCounter = counterForm.bindFromRequest.get
  	val counters = newCounter :: Cache.getAs[List[Counter]]("counters").getOrElse(Nil)
  	Cache.set("counters", counters)
  	Redirect(routes.CountersController.index)
  }

  def incrementCounter(id: String) = Action { implicit request =>
    val increment = incrementCounterForm.bindFromRequest.get
    Cache.set(id, increment.minutes + Cache.getAs[Int](id).getOrElse(0))
    Redirect(routes.CountersController.index)
  }

  val counterForm = Form(
  		mapping(
  			"name" -> text
  		)(Counter.apply)(Counter.unapply)
  	)

  val incrementCounterForm = Form(
      mapping(
        "minutes" -> number
      )(CounterIncrement.apply)(CounterIncrement.unapply)
    )
}

object CountersController extends CountersControllerTrait with CounterRepositoryComponent {
  val counterRepository = new CounterRepository
}
