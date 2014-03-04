CouchDB plugin for Playframework
============================================

What this does
--------------

1. Provides a scala api for communicating with a couchdb server
2. Uses the play builtin async Ws request api (all operations are non-blocking)
3. Create, Replace, Delete and Get operations
4. Update your database design and static documents from source control as evolutions does for sql DBs
5. Directory -> json mapper. Js functions in design documents can be their own .js files getting editor syntax highlighting and line diffs for commits.

Prerequisites
-------------

1. Get play framework (http://www.playframework.com/) - (2.2.0 tested)
2. Install CouchDB - packages available on linux / (http://couchdb.apache.org/) - (1.2.0 & 1.4.0 tested)

Install module from source
--------------------------

3. Get the couchplayplugin source `git clone https://github.com/platy/couchplayplugin.git`
4. `cd couchplayplugin/module`
5. Run the tests and build the module to your local repository `play test publish-local`

    By default, the tests run against host=http://localhost:5984 database=scala-couch-test with no auth credentials. Specify credentials or a different host etc on the command line eg. `play -Dcouch.user=user -Dcouch.password=password test`

Try the demo
------------
WARNING - UI is crap and doesn't demonstrate well the power of couchdb or play

1. (after Getting started) `cd ../samples/time-counter/`
2. If your couch instance has admin access protected, you will need to create a database and user and provide the details to the run command `-Dcouch.db.default.{host,database,user,password}=` or change the application.conf file
2. Run tests and launch sample `play test run`
3. Go to http://localhost:9000/
4. You will be asked to create the database and sync
5. You can then create counters and add to them

Add to your play project
------------------------

build.sbt:

    libraryDependencies += "couch-play-plugin" % "couch-play-plugin_2.10" % "0.2"

conf/play.plugins: (assuming you want to use the document sync)

    451:couch.CouchPlayPlugin

conf/application.conf:

    couch.db.default.host="http://localhost:5984/"
    couch.db.default.database="dbname"
    couch.db.default.dir="conf/couch"

conf/couch/: Add json documents to sync - must have .json extension, docs in a _design/ subdir will become design docs

I don't currently have documentation for using the API but have a look at the sample usage - https://github.com/platy/couchplayplugin/blob/master/samples/time-counter/app/repositories/CounterRepositoryComponent.scala

Compatibility
-------------

Tested with scala 2.10, play 2.2.0 and couchDB 1.2.0 & 1.4.0, the parts of the couch api used by this library are very common and so are unlikely to change very much. I would assume that this works with many versions of couchdb.

Tests are provided which test against the local database and should bring up any compatibility problems.
