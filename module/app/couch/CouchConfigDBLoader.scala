package couch

import config.CouchConfiguration

class CouchConfigDBLoader(config: CouchConfiguration) {
  def apply(id: String): CouchDatabase = {
  	val dbConfig = config.db(id)
  	Couch.host(dbConfig.host).db(dbConfig.database)
  }
}
