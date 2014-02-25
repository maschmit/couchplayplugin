package couch.test

import couch.CouchConfigDBLoader
import couch.config._


trait TestCouchHost {
  
  val testCouchConfig = {
    val systemProp = new sys.SystemProperties()
    val host = systemProp.get("couch.host").getOrElse("http://localhost:5984/")
    val database = systemProp.get("couch.database").getOrElse("scala-couch-test")
    val credentials = for {
      u <- systemProp.get("couch.user")
   	  p <- systemProp.get("couch.pass")
    } yield (u, p)
    CouchDBConfiguration("test", host, database, None, credentials, false, false)
  }
  private val testLoader = new CouchConfigDBLoader(CouchConfiguration(Map("test" -> testCouchConfig)))
  val couchHost = testLoader.host("test")
}
