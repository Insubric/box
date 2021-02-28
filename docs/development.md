---
title: Development
permalink: /development/
nav_order: 5
---

# Development

BOX is completely written in Scala (Scala and Scala.js).

To extend the user interface a widgets system is in place

## Local server

```
sbt server/run
```
Serve task compiles both client (with fastOptJS) and server then starts the server

In order to continuously reload the UI changes and take advantage of webpack fast reloading devServer, enable `devServer` on the application.conf
```
devServer = true
```
be careful to enable it only in dev environment.

Enable the dev server with
```
sbt client/fastOptJS::startWebpackDevServer
``` 
doing so the compiled UI JavaScript are served through webpack devServer on port 8888.

Then set up the autocompile of the UI
```
~client/fastOptJS
```
that way every time a file is saved it get compilated and served on 8888.

Please note that there is no autoreload set up so you must reload the page manually on the browser.

### Pre generation of Table entities
running `sbt server/slick` tables files are generated so they are compiled only once, modification on database are ignored.

To delete the generate tables run `sbt server/deleteSlick`



## Modules

- codegen: Code generation from postgres database using slick codegen library
- server: Akka-Http REST server exposing tables of the db
- client: Web UI for the REST APIs
- shared: Shared code between client and server, not all the libraries used here must be compatible with both scala JVM and Scala.js

## Libraries


- [Akka-http](https://doc.akka.io/docs/akka-http/current/)
- [Slick](http://slick.lightbend.com/)
- [ScalaJS](http://www.scala-js.org/)
- [UDash](http://udash.io/)


### Knows Issues


If on compile time `StackOverflow` errors appears use the following parameters:
```
sbt -J-Xmx4G -J-XX:MaxMetaspaceSize=1G -J-XX:MaxPermSize=1G -J-XX:+CMSClassUnloadingEnabled -J-Xss3m serve
```

# Client

## Js dependency management

JavaScript dependencies are managed using webpack npm and [https://scalablytyped.org](https://scalablytyped.org).
Js dependency are injected in the bundle by webpack, if some css file is needed the library need to be exposed and loaded manually (this could be improved)