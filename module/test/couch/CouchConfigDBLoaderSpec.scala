package couch

import config._

import org.scalatest._


class CouchConfigDBLoaderSpec extends FlatSpec with ShouldMatchers with GivenWhenThen {
  val testDbConfig = CouchDBConfiguration("id", "http://testhost/", "testDb", Some("syncDir"), false, false)
  val configuration = CouchConfiguration(Map("id" -> testDbConfig))
  
  "CouchConfigDBLoader(id)" should "get a database configured with that id" in {
  	Given("the loader is configured with a CouchDB configuration")
    val loader = new CouchConfigDBLoader(configuration)
  	When("the loader is used to load a db")
    val db: CouchDatabase = loader("id")
  	Then("the db is configured from the config")
  	db.url should be ("http://testhost/testDb")
  }

}
