package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.Play.current

import models._
import repositories.CounterRepositoryComponent
import play.api.libs.concurrent.Execution.Implicits.defaultContext


trait CountersControllerTrait extends Controller {
  this: CounterRepositoryComponent =>

  def index = Action.async {
    counterRepository.counters.map(
      counters => Ok(views.html.counters(counterForm, incrementCounterForm, counters))
    )
  }

  def addCounter = Action.async { implicit request =>
  	val newCounter = counterForm.bindFromRequest.get
    counterRepository.add(newCounter).map (
  	  _ => Redirect(routes.CountersController.index)
    )
  }

  def incrementCounter(id: String) = Action.async { implicit request =>
    val increment = incrementCounterForm.bindFromRequest.get
    counterRepository.increment(CounterId(id), increment).map (
      _ => Redirect(routes.CountersController.index)
    )
  }

  val counterForm = Form(
  		mapping(
  			"name" -> text
  		)(Counter.apply)(Counter.unapply)
  	)

  val incrementCounterForm = Form(
      mapping(
        "minutes" -> number
      )(Time.apply)(Time.unapply)
    )
}

object CountersController extends CountersControllerTrait with CounterRepositoryComponent {
  val counterRepository = new CounterRepository
}
