package couch.config

import play.api.Configuration

import java.io.File

import org.scalatest._


class CouchConfigurationSpec extends FlatSpec with ShouldMatchers with GivenWhenThen with BeforeAndAfter {
  val configMap = Map("couch" -> Map("db" -> Map(
    "primary" -> Map(
      "host" -> "http://hosturl/",
      "database" -> "databaseName",
      "autoApplyDev" -> true,
      "autoApplyProd" -> false,
      "checkSync" -> false
    ),
    "secondary" -> Map(
      "host" -> "https://anotherHost/",
      "database" -> "anotherDatabaseName"
    )
  )))
  val config = Configuration.from(configMap)

  "CouchConfiguration" should "construct objects for dbs from config" in {
    val couchConfig = CouchConfiguration(config)
    val firstConfig = couchConfig.db("primary")
    firstConfig.host should be ("http://hosturl/")
    firstConfig.database should be ("databaseName")
    firstConfig.autoApplyDev should be (true)
    firstConfig.autoApplyProd should be (false)
    firstConfig.checkSync should be (false)

    val secondConfig = couchConfig.db("secondary")
    secondConfig.host should be ("https://anotherHost/")
    secondConfig.database should be ("anotherDatabaseName")
    secondConfig.autoApplyDev should be (false)
    secondConfig.autoApplyProd should be (false)
    secondConfig.checkSync should be (true)
  }
}
