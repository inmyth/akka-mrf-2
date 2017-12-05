## Mr Filter 2

MrFilter 2 adds API key to secret key conversion filter. It also blocks tx_blob from request.

Use application.conf with akka.http.server.idle-timeout = infinite to prevent disconnect. Put the file in `src/main/resources`. This file only works when run with sbt. With eclipse, classpath seems to point somewhere else.

This project also contains MrFilter2 in vertx.

v.201 Working version