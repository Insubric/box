//addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.0")

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")



addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"  % "1.0.0")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta32")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.20.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.1.0"
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
libraryDependencies += "com.browserstack" % "browserstack-local-java" % "1.0.3"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-firefox-driver" % "3.141.59"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59"
