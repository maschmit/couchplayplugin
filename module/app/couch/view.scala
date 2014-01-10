package couch.view

import couch.error.ClientImplementationException

import play.api.libs.json._
import play.api.libs.functional.syntax._


object ViewResult {
  import ViewReaders._
  def apply(total_rows: Option[Int], offset: Option[Int], rows: JsArray) = (total_rows, offset) match {
    case (Some(rc), Some(o)) => MapViewResult(rc, o, rows.as[List[MapViewElement]])
    case (None, None)        => ReduceViewResult(rows.as[List[ReduceViewElement]])
    case v => throw new ClientImplementationException(s"Expected either both total_rows and offset or neither, got: $v")
  }  
}
abstract class ViewResult {
  type Element <: ViewElement
  def rows: List[Element]
  protected lazy val keysMulti: Map[JsValue, Iterable[Element]] = 
    rows.groupBy(_.key)
}
case class MapViewResult(rowCount: Int, offset: Int, override val rows: List[MapViewElement]) extends ViewResult {
  type Element = MapViewElement
}
case class ReduceViewResult(override val rows: List[ReduceViewElement]) extends ViewResult {
  type Element = ReduceViewElement
  def key(k: JsValue): ReduceViewElement = keysMulti(k).iterator.next
}

abstract class ViewElement {
	def key: JsValue
	def value: JsValue
}
case class MapViewElement(id: String, override val key: JsValue, override val value: JsValue) extends ViewElement
case class ReduceViewElement(override val key: JsValue, override val value: JsValue) extends ViewElement


object ViewReaders {
  implicit val reduceViewElement: Reads[ReduceViewElement] = (
      (__ \ "key").read[JsValue] ~
      (__ \ "value").read[JsValue]
    )(ReduceViewElement)

  implicit val mapViewElement: Reads[MapViewElement] = (
      (__ \ "id").read[String] ~
      (__ \ "key").read[JsValue] ~
      (__ \ "value").read[JsValue]
    )(MapViewElement)

  implicit val viewResultReads: Reads[ViewResult] = (
      (__ \ "total_rows").readNullable[Int] ~
      (__ \ "offset").readNullable[Int] ~
      (__ \ "rows").read[JsArray]
    )(ViewResult.apply _)
}
