package couch

import config._
import sync._

import play.core._
import play.api._

import java.io.File

import org.scalatest._


class CouchPlayPluginSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val testDbConfig = CouchDBConfiguration("id", "http://testhost/", "testDb", None, true, false, false)
  
  

  class CouchPlayPlugin(config: CouchConfiguration, mode: Mode.Mode, testSyncResult: MultiMatchResult) extends Plugin with HandleWebCommandSupport with BaseCouchPlayPlugin {
    def couchConfig = config
    def appMode = mode
    def testSync(localDir: String, db: Couch.CouchDatabase): MultiMatchResult = testSyncResult
      
  }
}
