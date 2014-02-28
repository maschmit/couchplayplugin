package couch.view

import play.api.libs.json._


/** Result from a view query on a design.
  * Depending on the type of query and whether a reduce is present in the design this may be a [[MapViewResult]] or [[ReduceViewResult]]
  */
abstract class ViewResult {
  /** The type of the contained elements will vary between [[MapViewResult]] and [[ReduceViewResult]] depending on the type of result */
  type Element <: ViewElement

  /** 0 or more elements from the result */
  def rows: List[Element]

  /** key -> element: [[ViewElement]]
    *
    * For a reduce query this is one-to-one.
    * 
    * For a map query this is one-to-many.
    */
  protected lazy val keysMulti: Map[JsValue, Iterable[Element]] = 
    rows.groupBy(_.key)
}

/** Result from a Map query on a design document.
  *
  * @param rowCount number of rows in this response
  * @param offset offset of result
  */
case class MapViewResult(rowCount: Int, offset: Int, override val rows: List[MapViewElement]) extends ViewResult {
  type Element = MapViewElement
}

/** Result from a Reduce query on a design document.
  */
case class ReduceViewResult(override val rows: List[ReduceViewElement]) extends ViewResult {
  type Element = ReduceViewElement

  /** Returns element for specified key
    * 
    * @throws NoSuchElementException if key does not exist
    */
  def key(k: JsValue): ReduceViewElement = keysMulti(k).iterator.next
}
