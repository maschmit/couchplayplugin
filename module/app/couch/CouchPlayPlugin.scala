package couch

import config._
import sync._
import play.core._

import play.api._
import play.api.libs.json.Json
import play.api.libs._
import scala.util.control.Exception._
import scala.util.control.NonFatal
import play.api.libs.concurrent.Execution.Implicits.defaultContext // TODO : implicitly pass ins

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import java.io.File

/*
Note to use this plugin, you need to add a conf/play.plugins file to your app that looks like

451:couch.CouchPlayPlugin
*/
class CouchPlayPlugin(app: Application) extends Plugin with HandleWebCommandSupport {

  def couchConfig = CouchConfiguration(app.configuration)
  def applyEvolutions = false
  /**
   * Checks the evolutions state.
   */
  override def onStart() {

    couchConfig.db.values.foreach { dbConfig =>
      val syncDir = dbConfig.syncDir.getOrElse("conf/couch")
      val db = Couch(dbConfig.host).db(dbConfig.database)
      val script = Await.result(CouchSync(new File(syncDir)).check(db), 1.second)
      println(script)

      if (!script.isMatch) {
        app.mode match {
          case Mode.Test => script.run()
          case Mode.Dev if applyEvolutions => script.run()
          case Mode.Prod if applyEvolutions => script.run()
          case Mode.Prod => {
            Logger.warn("Your production database [" + dbConfig.id + "] needs evolutions! \n\n" + toHumanReadableScript(script))
            //Logger.warn("Run with -DapplyEvolutions." + dbName + "=true if you want to run them automatically (be careful)")

            throw InvalidDatabaseRevision(dbConfig.id, toHumanReadableScript(script))
          }
          case _ => throw InvalidDatabaseRevision(dbConfig.id, toHumanReadableScript(script))
        }
      }
    }
  }

  def toHumanReadableScript(results: Seq[MatchResult] with MatchResult): String = 

  	<div>
  	  <div style="width: 50%; display: inline-block"><h3>Source</h3></div><div style="width: 50%; display: inline-block"><h3>Destination</h3></div>
  	</div>.toString +
  	results.collect { case result: DocumentNoMatch =>
    <div>
      <h4>{result.source} ==> {result.destination.url}</h4>
      <div style="width: 49%; display: inline-block">
        <pre style="overflow-x: scroll">{Json.prettyPrint(result.sourceJson)}</pre>
      </div>
      <div style="width: 49%; display: inline-block">
        <pre style="overflow-x: scroll">{result.destinationJson.map(_.json).map(Json.prettyPrint(_)).getOrElse("")}</pre>
      </div>
    </div>
  }.mkString

  def handleWebCommand(request: play.api.mvc.RequestHeader, sbtLink: play.core.SBTLink, path: java.io.File): Option[play.api.mvc.SimpleResult] = {

    val applyEvolutions = """/@couchplay/apply/([a-zA-Z0-9_]+)""".r 

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")

    request.path match {

      case applyEvolutions(dbId) => {
        Some {
          val dbConfig = couchConfig.db(dbId)
		  val db = Couch(dbConfig.host).db(dbConfig.database)
		  val script = Await.result(CouchSync(new File("conf/couch")).to(db), 1.second)
	      sbtLink.forceReload()
	      play.api.mvc.Results.Redirect(redirectUrl)
        }
      }

      case _ => None

    }

  }

}


/**
 * Exception thrown when the database is not up to date.
 *
 * @param db the database name
 * @param script the script to be run to resolve the conflict.
 */
case class InvalidDatabaseRevision(db: String, script: String) extends PlayException.RichDescription(
  "Database '" + db + "' needs to be synced!",
  "There is a difference between documents on the database and in the local source directory.") {

  def subTitle = "These doc(s) must be updated"

  def content = ""

  private val javascript = """
        document.location = '/@couchplay/apply/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {

    <span>Document(s) will be updated on your database -</span>
    <input name="evolution-button" type="button" value="Update document(s) now!" onclick={ javascript }/>

  }.mkString + script

}