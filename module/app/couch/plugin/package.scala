package couch

/** Classes used to implement the automatic database sync on server startup.
  *
  * Note to use this plugin, you need to add a conf/play.plugins file to your app that looks like
  *
  * {{{
  * 451:couch.plugin.CouchPlayPlugin
  * }}}
  *
  * Then you can have any number of database configurations in your application.conf which sync from the source, ie.
  * 
  * {{{
  * couch.db.<db id>.host="http://localhost:5984/"
  * couch.db.<db id>.database="databse intance name"
  * couch.db.<db id>.username="username"
  * couch.db.<db id>.password="password"
  * couch.db.<db id>.dir="conf/couch"
  * }}}
  */
package object plugin {
}
