package couch

import config.CouchConfiguration

trait CouchDBLoader {
  def db(id: String): CouchDatabase
  def host(id: String): CouchHost
}

class CouchConfigDBLoader(config: CouchConfiguration) extends CouchDBLoader {
  def db(id: String): CouchDatabase = {
  	val dbConfig = config.db(id)
  	host(id).db(dbConfig.database)
  }
  
  def host(id: String): CouchHost = {
  	val dbConfig = config.db(id)
  	val host = Couch.host(dbConfig.host)
  	dbConfig.credentials match {
  	  case None => host
  	  case Some((user, pass)) => host.user(user, pass)
  	}
  }
}
