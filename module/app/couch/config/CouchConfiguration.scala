package couch.config

import play.api.Configuration

object CouchConfiguration {

  def apply(config: Configuration): CouchConfiguration = {
  	val dbs = for {
      couchConfig <- config.getConfig("couch.db").toSeq
      dbName <- couchConfig.subKeys
      dbConfig <- couchConfig.getConfig(dbName)
      dbHost <- dbConfig.getString("host")
      dbDatabase <- dbConfig.getString("database")
    } yield (dbName, new CouchDBConfiguration(dbName, dbHost, dbDatabase, dbConfig.getString("dir"),
	  dbConfig.getBoolean("autoApplyDev").getOrElse(false), dbConfig.getBoolean("autoApplyProd").getOrElse(false)))
    CouchConfiguration(dbs.toMap)
  }
}

case class CouchConfiguration(db: Map[String, CouchDBConfiguration])

case class CouchDBConfiguration(id: String, host: String, database: String, syncDir: Option[String],
	autoApplyDev: Boolean, autoApplyProd: Boolean) {
	def checkSync = syncDir.isDefined
}
