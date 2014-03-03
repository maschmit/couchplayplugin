package couch.directorymapper

import java.io.File

import play.api.libs.json._

/** Maps a file or directory to a json value
  *
  * A file with extension .json is parsed to a JsValue.
  *
  * A file with any other extension is read to a JsString.
  *
  * A directory is loaded as a JsObject with fields for each of it's children where the
  * key is the base of the filename and the value is the value loaded for the file using
  * these 3 rules.
  */
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
