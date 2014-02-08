package couch.config

import play.api.{Configuration,Play}
import Play.current

object CouchConfiguration {
  
  lazy val config = apply(Play.configuration)

  def apply(config: Configuration): CouchConfiguration = {
  	val dbs = for {
      couchConfig <- config.getConfig("couch.db").toSeq
      dbName <- couchConfig.subKeys
      dbConfig <- couchConfig.getConfig(dbName)
      dbHost <- dbConfig.getString("host")
      dbDatabase <- dbConfig.getString("database")
    } yield (dbName, new CouchDBConfiguration(dbName, dbHost, dbDatabase, dbConfig.getString("dir"),
      dbConfig.getString("username").map((_, dbConfig.getString("password").getOrElse(null))),
	  dbConfig.getBoolean("autoApplyDev").getOrElse(false), dbConfig.getBoolean("autoApplyProd").getOrElse(false)))
    CouchConfiguration(dbs.toMap)
  }
}

case class CouchConfiguration(db: Map[String, CouchDBConfiguration])

case class CouchDBConfiguration(id: String, host: String, database: String, syncDir: Option[String],
	credentials: Option[(String, String)], autoApplyDev: Boolean, autoApplyProd: Boolean) {
	def checkSync = syncDir.isDefined
}
