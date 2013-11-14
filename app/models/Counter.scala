package models

case class Counter(name: String)
case class CounterId(id: String)

case class CounterWithAggregate(counter: Counter, time: TimeCounter)

abstract class TimeCounter

object NoTime extends TimeCounter {
	override def toString = "no time"
}

case class Time(minutes: Int) extends TimeCounter {
	override def toString = s"$minutes minutes"
}

case class CounterIncrement(counterId: String, minutes: Int)
