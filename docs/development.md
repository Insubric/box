---
title: Development
permalink: /development/
nav_order: 9
layout: default
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


Then set up the autocompile of the UI
```
~client/fastOptJS::webpack
```
that way every time a file is saved it get compilated and bundled.

Please note that there is no autoreload set up so you must reload the page manually on the browser.


### Pre generation of Table entities
running `sbt server/slick` tables files are generated so they are compiled only once, modification on database are ignored.

To delete the generate tables run `sbt server/deleteSlick`


## Modules

- **codegen**: Code generation from postgres database using slick codegen library
- **server**: Akka-Http REST server exposing tables of the db
- **client**: Web UI for the REST APIs
- **shared**: Shared code between client and server, not all the libraries used here must be compatible with both scala JVM and Scala.js

## Libraries


- [Akka-http](https://doc.akka.io/docs/akka-http/current/)
- [Slick](http://slick.lightbend.com/)
- [ScalaJS](http://www.scala-js.org/)
- [UDash](http://udash.io/)


### Knows Issues


If on compile time `StackOverflow` errors appears use the following parameters:
```
sbt -J-Xmx4G -J-Xss3m serve
```

# Client

## Js dependency management

JavaScript dependencies are managed using webpack npm and [https://scalablytyped.org](https://scalablytyped.org).
Js dependency are injected in the bundle by webpack, if some css file is needed the library need to be exposed and loaded manually (this could be improved)

# Testing

## Server
We use [testcontainer](https://www.testcontainers.org/) for testing, so you need docker installed in your machine and the user running the tests should be able to create containers.

In linux if you get Permission error you may want open the socket (WARNING THAT HAS SOME IMPORTANT SECURITY DRAWBACK)
```
sudo chmod 666 /var/run/docker.sock
```

## Client
We use jsdom node.js implementation of the DOM, so you need to have node.js installed and jsdom:
```
npm install jsdom
```