package couch.view

import couch.error.ClientImplementationException

import play.api.libs.json._
import play.api.libs.functional.syntax._


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
    )(viewResult _)


  def viewResult(total_rows: Option[Int], offset: Option[Int], rows: JsArray) = (total_rows, offset) match {
    case (Some(rc), Some(o)) => MapViewResult(rc, o, rows.as[List[MapViewElement]])
    case (None, None)        => ReduceViewResult(rows.as[List[ReduceViewElement]])
    case v => throw new ClientImplementationException(s"Expected either both total_rows and offset or neither, got: $v")
  }
}
