package couch.directorymapper

import java.io.File

import play.api.libs.json._

object JsonFSMapper {
	def load(f: File): JsValue = f match {
	  case file if file.isFile && hasJsonExtension(file) => Json.parse(io.Source.fromFile(file).mkString)
	  case file if file.isFile => new JsString(io.Source.fromFile(file).mkString)
	  case dir if dir.isDirectory => dirToObj(dir)
	}

	private def dirToObj(dir: File) = Json.obj(
	  dir.listFiles.map { file =>
	    (keyName(file), Json.toJsFieldJsValueWrapper(load(file)))
	  }: _*
	)

	private def hasJsonExtension(file: File) = file.getName.endsWith(".json")

	private def keyName(file: File) = {
	  def keyName(name: String) = name.lastIndexOf('.') match {
	  	case -1 => name // No extension, use entire names
	    case index => name.substring(0, index)
	  }
	  keyName(file.getName)
	}
}