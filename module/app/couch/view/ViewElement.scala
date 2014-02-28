package couch.view

import play.api.libs.json._


abstract class ViewElement {
	def key: JsValue
	def value: JsValue
}
case class MapViewElement(id: String, override val key: JsValue, override val value: JsValue) extends ViewElement
case class ReduceViewElement(override val key: JsValue, override val value: JsValue) extends ViewElement
