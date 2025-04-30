//addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.0")

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")

addSbtPlugin("io.github.cquiroz" % "sbt-locales" % "4.2.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.5.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"  % "1.0.0")

addSbtPlugin("com.lumidion"   % "sbt-sonatype-central"  % "0.2.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.0")

addDependencyTreePlugin

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.1.0"
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
libraryDependencies += "com.browserstack" % "browserstack-local-java" % "1.0.3"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-firefox-driver" % "3.141.59"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59"
