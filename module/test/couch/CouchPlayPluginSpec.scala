package couch

import config._
import sync._

import play.core._
import play.api._

import java.io.File

import org.scalatest._


class CouchPlayPluginSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val testDbConfig = CouchDBConfiguration("id", "http://testhost/", "testDb", Some("syncDir"), false, false)
  
  "CouchPlayPlugin startup" should "automatically apply updates when in test mode" in {
  	Given("the plugin is configured with a CouchDB configuration and is started in test mode")
  	And("the result of the sync check is that the db needs updating")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig)), Mode.Test, testSyncResult)
  	When("the plugin is started")
  	plugin.onStart()
  	Then("the update will run")
  	testSyncResult.updateRan should be (true)
  }

  it should "automatically apply updates when in dev mode with autoApplyDev enabled" in {
  	Given("the plugin is configured with a CouchDB configuration and is started in dev mode")
  	And("the result of the sync check is that the db needs updating")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig.copy(autoApplyDev = true))), Mode.Dev, testSyncResult)
  	When("the plugin is started")
  	plugin.onStart()
  	Then("the update will run")
  	testSyncResult.updateRan should be (true)
  }

  it should "automatically apply updates when in prod mode with autoApplyProd enabled" in {
  	Given("the plugin is configured with a CouchDB configuration and is started in prod mode")
  	And("the result of the sync check is that the db needs updating")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig.copy(autoApplyProd = true))), Mode.Prod, testSyncResult)
  	When("the plugin is started")
  	plugin.onStart()
  	Then("the update will run")
  	testSyncResult.updateRan should be (true)
  }

  it should "throw an exception in prod mode with autoApplyProd disabled" in {
  	Given("the plugin is configured with a CouchDB configuration and is started in prod mode")
  	And("the result of the sync check is that the db needs updating")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig)), Mode.Prod, testSyncResult)
  	When("the plugin is started")
  	val exception = intercept[RemoteDocsOutOfSync] {
  		plugin.onStart()
  	}
  	Then("the update will not be run")
  	testSyncResult.updateRan should be (false)
  	And("the exception will contain the sync result and db id")
  	exception.db should be ("id")
  	exception.results should be (testSyncResult)
  }

  it should "throw an exception in dev mode with autoApplyDev disabled" in {
  	Given("the plugin is configured with a CouchDB configuration and is started in dev mode")
  	And("the result of the sync check is that the db needs updating")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig)), Mode.Dev, testSyncResult)
  	When("the plugin is started")
  	val exception = intercept[RemoteDocsOutOfSync] {
  		plugin.onStart()
  	}
  	Then("the update will not be run")
  	testSyncResult.updateRan should be (false)
  	And("the exception will contain the sync result and db id")
  	exception.db should be ("id")
  	exception.results should be (testSyncResult)
  }

  it should "do nothing if the syncDir is not defined" in {
  	Given("the plugin is configured with a CouchDB configuration with no syncDir and is started in test mode")
  	val testSyncResult = new TestSyncResult(false)
  	val plugin = new CouchPlayPlugin(CouchConfiguration(Map("id" -> testDbConfig.copy(syncDir = None))), Mode.Test, testSyncResult)
  	When("the plugin is started")
  	plugin.onStart()
  	Then("the update will not run")
  	testSyncResult.updateRan should be (false)
  }

  class CouchPlayPlugin(config: CouchConfiguration, mode: Mode.Mode, testSyncResult: MultiMatchResult) extends Plugin with HandleWebCommandSupport with BaseCouchPlayPlugin {
    def couchConfig = config
    def appMode = mode
    def testSync(localDir: String, db: Couch.CouchDatabase): MultiMatchResult = testSyncResult
      
  }

  class TestSyncResult(matches: Boolean) extends collection.SeqProxy[SingleMatchResult] with MultiMatchResult {
  	  var updateRan = false
      def isMatch: Boolean = matches
      def run(): scala.concurrent.Future[Seq[couch.document.DocumentHeader]] = { updateRan = true; scala.concurrent.Future.successful(Nil) }
      def self: Seq[couch.sync.SingleMatchResult] = Nil
  	}
}
