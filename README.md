Time counter example for CouchDB play plugin
============================================

What this does
--------------

1. Provides a scala api for communicating with a couchdb server
2. Uses the play builtin async Ws request api (all operations are non-blocking)
3. Create, Replace, Delete and Get operations

Future things
-------------

1. Add a startup hook to check that the design docs on the couch server match those in the conf directory
2. Make uploading updated design documents easy
3. CouchApp / kanso like directory - design doc mapping
4. Test against various versions to find the compatibility bounds
5. Set up automated testing
6. take implicit write on create etc so it can take T instead of JsValue

Compatibility
-------------

Tested with play 2.10 and couchDB 1.2.0, the parts of the couch api used by this library are very common and so are unlikely to change very much. I would assume that this works with many versions of couchdb.

Tests are provided which test against the local database and should bring up any compatibility problems.