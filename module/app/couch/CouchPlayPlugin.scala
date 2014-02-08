package couch

import error.DatabaseNotFound
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
class CouchPlayPlugin(app: Application) extends Plugin with HandleWebCommandSupport with BaseCouchPlayPlugin {

  lazy val couchConfig = CouchConfiguration(app.configuration)
  lazy val loadCoach = new CouchConfigDBLoader(couchConfig)
  def appMode = app.mode
  def testSync(localDir: String, db: CouchDatabase): MultiMatchResult =
    Await.result(CouchSync(new File(localDir)).check(db), 1.second)

}

trait BaseCouchPlayPlugin extends Plugin with HandleWebCommandSupport {
  def couchConfig: CouchConfiguration
  def appMode: Mode.Mode
  def testSync(localDir: String, db: CouchDatabase): MultiMatchResult
  def loadCoach: CouchDBLoader

  /**
   * Checks the evolutions state.
   */
  override def onStart() {

    couchConfig.db.values.filterNot(_.syncDir == None).foreach { dbConfig =>
      val db = loadCoach.db(dbConfig.id)
      syncDB(dbConfig, db)
      syncDocs(dbConfig, db)
    }

    def syncDB(dbConfig: CouchDBConfiguration, db: CouchDatabase) {
      def createDB() = loadCoach.host(dbConfig.id).addDb(dbConfig.database)
      Await.result(db.info().recoverWith {
      	case _: DatabaseNotFound => appMode match {
          case Mode.Test => createDB()
          case Mode.Dev if dbConfig.autoApplyDev => createDB()
          case Mode.Prod if dbConfig.autoApplyProd => createDB()
          case Mode.Prod => {
            Logger.warn(s"Your production database [${dbConfig.id}] needs to be created! (${db.url}) \n\n")

            throw RemoteDBDoesntExist(dbConfig.id, db.url)
          }
          case _ => throw RemoteDBDoesntExist(dbConfig.id, db.url)
        }
        db.info()
      }, 1.second)
    }

    def syncDocs(dbConfig: CouchDBConfiguration, db: CouchDatabase) {
      val syncDir = dbConfig.syncDir.get
      val script = testSync(syncDir, db)

      if (!script.isMatch) {
        appMode match {
          case Mode.Test => script.run()
          case Mode.Dev if dbConfig.autoApplyDev => script.run()
          case Mode.Prod if dbConfig.autoApplyProd => script.run()
          case Mode.Prod => {
            Logger.warn("Your production database [" + dbConfig.id + "] needs evolutions! \n\n" + script.map(_.destination.url).mkString)

            throw RemoteDocsOutOfSync(dbConfig.id, script)
          }
          case _ => throw RemoteDocsOutOfSync(dbConfig.id, script)
        }
      }
    }
  }

  def handleWebCommand(request: play.api.mvc.RequestHeader, sbtLink: play.core.SBTLink, path: java.io.File): Option[play.api.mvc.SimpleResult] = {

    val applyEvolutions = """/@couchplay/apply/([a-zA-Z0-9_]+)""".r 
    val createDB = """/@couchplay/create/([a-zA-Z0-9_]+)""".r 

    lazy val redirectUrl = request.queryString.get("redirect").filterNot(_.isEmpty).map(_(0)).getOrElse("/")

    request.path match {

      case applyEvolutions(dbId) => Some {
    	  val db = loadCoach.db(dbId)
		    Await.result(CouchSync(new File("conf/couch")).to(db), 1.second)
	      sbtLink.forceReload()
	      play.api.mvc.Results.Redirect(redirectUrl)
      }

      case createDB(dbId) => Some {
        val dbConfig = couchConfig.db(dbId)
        val host = loadCoach.host(dbId)
	      println(Await.result(host.addDb(dbConfig.database), 1.second))
        sbtLink.forceReload()
        play.api.mvc.Results.Redirect(redirectUrl)
      }

      case _ => None
    }

  }

}


/**
 * Exception thrown when the syncing database doesn't exist
 *
 * @param db the database name
 */
case class RemoteDBDoesntExist(db: String, url: String) extends PlayException.RichDescription(
  "Database '" + db + "' doesn't exist!",
  "A CouchDB database configured for this application doesn't exist.") {

  def subTitle = "The database must be created"

  def content = s"Database must be created at $url"

  private val javascript = """
        document.location = '/@couchplay/create/%s?redirect=' + encodeURIComponent(location)
    """.format(db).trim

  def htmlDescription = {

    <span>A database will be created on your couchDB instance -</span>
    <input name="evolution-button" type="button" value="Create database!" onclick={ javascript }/>

  }.mkString

}


/**
 * Exception thrown when the database is not up to date.
 *
 * @param db the database name
 * @param script the script to be run to resolve the conflict.
 */
case class RemoteDocsOutOfSync(db: String, results: Seq[MatchResult] with MatchResult) extends PlayException.RichDescription(
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

  }.mkString + toHumanReadableScript(results)

  private def toHumanReadableScript(results: Seq[MatchResult] with MatchResult): String = 

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

}