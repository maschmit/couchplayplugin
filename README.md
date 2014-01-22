CouchDB plugin for Playframework
============================================

What this does
--------------

1. Provides a scala api for communicating with a couchdb server
2. Uses the play builtin async Ws request api (all operations are non-blocking)
3. Create, Replace, Delete and Get operations
4. (COMING SOON) update your database design and static documents from source control as evolutions does for sql DBs

Compatibility
-------------

Tested with play 2.10 and couchDB 1.2.0, the parts of the couch api used by this library are very common and so are unlikely to change very much. I would assume that this works with many versions of couchdb.

Tests are provided which test against the local database and should bring up any compatibility problems.
