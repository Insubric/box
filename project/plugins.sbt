//addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")



addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"  % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta32")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.20.0")

addDependencyTreePlugin

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "1.1.0"
libraryDependencies += "net.exoego" %% "scalajs-env-jsdom-nodejs" % "2.1.0"
libraryDependencies += "com.browserstack" % "browserstack-local-java" % "1.0.3"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-firefox-driver" % "3.141.59"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59"
