package couch.config

import play.api.Configuration

import java.io.File

import org.scalatest._


class CouchConfigurationSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val configMap = Map("couch" -> Map("db" -> Map(
    "primary" -> Map(
      "host" -> "http://hosturl/",
      "database" -> "databaseName",
      "username" -> "user",
      "password" -> "pass",
      "autoApplyDev" -> true,
      "autoApplyProd" -> false,
      "dir" -> "syncDir"
    ),
    "secondary" -> Map(
      "host" -> "https://anotherHost/",
      "database" -> "anotherDatabaseName"
    )
  )))
  val config = Configuration.from(configMap)
  val couchConfig = CouchConfiguration(config)

  "CouchConfiguration" should "construct objects for dbs from config" in {
    val firstConfig = couchConfig.db("primary")
    firstConfig.host should be ("http://hosturl/")
    firstConfig.database should be ("databaseName")
    firstConfig.credentials should be (Some(("user", "pass")))
    firstConfig.autoApplyDev should be (true)
    firstConfig.autoApplyProd should be (false)
    firstConfig.checkSync should be (true)
    firstConfig.syncDir should be (Some("syncDir"))
  }

  it should "construct the correct default values" in {
    val secondConfig = couchConfig.db("secondary")
    secondConfig.host should be ("https://anotherHost/")
    secondConfig.database should be ("anotherDatabaseName")
    secondConfig.credentials should be (None)
    secondConfig.autoApplyDev should be (false)
    secondConfig.autoApplyProd should be (false)
    secondConfig.checkSync should be (false)
    secondConfig.syncDir should be (None)
  }
}
