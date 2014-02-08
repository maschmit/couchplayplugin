package couch

import config._

import org.scalatest._


class CouchConfigDBLoaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen {
  val testDbConfig = CouchDBConfiguration("id", "http://testhost/", "testDb", Some("syncDir"), None, false, false)
  val configuration = CouchConfiguration(Map("id" -> testDbConfig))
  
  "CouchConfigDBLoader.db(id)" should "get a database configured with that id" in {
  	Given("the loader is configured with a CouchDB configuration")
    val loader = new CouchConfigDBLoader(configuration)
  	When("the loader is used to load a db")
    val db: CouchDatabase = loader.db("id")
  	Then("the db is configured from the config")
  	db.url should be ("http://testhost/testDb")
  }
  
  "CouchConfigDBLoader.host(id)" should "get a host configured with that id" in {
    Given("the loader is configured with a CouchDB configuration")
    val loader = new CouchConfigDBLoader(configuration)
    When("the loader is used to load a host")
    val host: CouchHost = loader.host("id")
    Then("the host is configured from the config")
    host.url should be ("http://testhost")
  }

}
