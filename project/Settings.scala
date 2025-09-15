import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

//scalaJs version is set into the plugin.sbt file


/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {




  val scalacOptions = Seq(
    "-feature",
    "-language:postfixOps"
  )

  /** Options for the scala compiler */
  val scalacOptionsServer = scalacOptions ++ Seq(
    "-Yrangepos",
    "-language:existentials"
  )




  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {

    //General
    val scala213 = "2.13.16"
    val ficus = "1.5.2"

    val macWire = "2.3.7"
    val airframe = "22.9.3"

    //HTTP actors
    val akka = "2.6.4"
    val akkaHttp = "10.2.10"
    val akkaHttpJson = "1.39.2"

    //Testing
    val specs2 = "4.3.4"
    val junit = "4.12"
    val scalatest = "3.2.13"
    val selenium = "3.14.0"
    val testcontainersScalaVersion = "0.40.10"


    //json parsers
    val circe = "0.14.3"

    //database
    val postgres = "42.2.20"
    val slick = "3.4.1"
    val slickPg = "0.21.0"
    val flyway = "10.7.1"

    //frontend
    val scalaCss = "1.0.0"

    //js
    val bootstrap =  "3.4.1-1"

    val udash = "0.9.0-M39"
    val udashJQuery = "3.0.4"

    val scribe = "3.0.2"


    val scalaJsonSchema = "0.2.6"

    val kantan = "0.6.1"


  }

  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedJVMJSDependencies = Def.setting(Seq(
    //"io.udash" %%% "udash-core" % versions.udash,
    "io.circe" %%% "circe-core" % versions.circe,
    "io.circe" %%% "circe-generic" % versions.circe,
    "io.circe" %%% "circe-parser" % versions.circe,
    "io.circe" %%% "circe-generic-extras" % versions.circe,
    "com.outr" %%% "scribe" % versions.scribe,
    "com.nrinaudo" %%% "kantan.csv" % versions.kantan,
    "com.github.eikek" %%% "yamusca-core" % "0.8.0",
  ))

  val sharedJVMCodegenDependencies = Def.setting(Seq(
    "com.typesafe.slick"       %% "slick"           % versions.slick,
    "com.typesafe.slick"       %% "slick-hikaricp"           % versions.slick,
    "org.postgresql"           %  "postgresql"      % versions.postgres,
    "com.typesafe"             % "config"           % "1.4.2",
    "com.iheart"               %% "ficus"           % versions.ficus,
    "com.github.tminglei"      %% "slick-pg"         % versions.slickPg,
    "com.github.tminglei"      %% "slick-pg_jts_lt"     % versions.slickPg,
    "io.circe"                 %% "circe-core" % versions.circe,
    "com.github.tminglei"      %% "slick-pg_circe-json"     % versions.slickPg,
    "org.locationtech.jts" % "jts-core" % "1.16.1",
    "com.dimafeng"             %% "testcontainers-scala-postgresql" % versions.testcontainersScalaVersion,
    "org.flywaydb" % "flyway-core" % versions.flyway,
    "org.flywaydb" % "flyway-database-postgresql" % versions.flyway,
    "com.outr"                 %% "scribe"           % versions.scribe,
    "com.outr"                 %% "scribe-slf4j18"           % versions.scribe,
  ))

  val codegenDependecies = Def.setting(sharedJVMCodegenDependencies.value ++ Seq(
    "com.typesafe.slick" %% "slick-codegen" % versions.slick,
    "com.outr" %% "scribe" % versions.scribe,
  ))

  val serverCacheRedisDependecies = Def.setting(Seq(
    "com.github.scredis" %% "scredis" % "2.4.3",
    "com.iheart"         %% "ficus"   % versions.ficus,
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(sharedJVMCodegenDependencies.value ++ Seq(
    "org.scala-lang"           % "scala-reflect"     % versions.scala213,
    "org.scala-lang"           % "scala-compiler"    % versions.scala213,
    "com.typesafe.akka"        %% "akka-http-core"   % versions.akkaHttp,
    "com.typesafe.akka"        %% "akka-http-caching" % versions.akkaHttp,
    "de.heikoseeberger"        %% "akka-http-circe"  % versions.akkaHttpJson,
    "com.typesafe.akka"        %% "akka-actor"       % versions.akka,
    "com.typesafe.akka"        %% "akka-stream"      % versions.akka,
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.5-akka-2.6.x",
    "com.softwaremill.akka-http-session" %% "core"   % "0.5.11",
    "io.circe"                 %% "circe-core"       % versions.circe,
    "io.circe"                 %% "circe-generic"    % versions.circe,
    "io.circe"                 %% "circe-parser"     % versions.circe,
    "org.webjars"               % "webjars-locator-core" % "0.44",
    "org.webjars"              % "webjars-locator"   % "0.39",
    //"org.specs2"               %% "specs2-core"      % versions.specs2    % "test",
    "junit"                    %  "junit"            % versions.junit     % "test",
    "org.seleniumhq.selenium"  %  "selenium-java"    % versions.selenium  % "test",
    "com.typesafe.akka"        %% "akka-testkit"     % versions.akka      % "test",
    "com.typesafe.akka"        %% "akka-http-testkit"% versions.akkaHttp  % "test",
    "com.dimafeng"             %% "testcontainers-scala-scalatest" % versions.testcontainersScalaVersion % "test",
    "ch.wavein"                %% "scala-thumbnailer" % "0.7.2",
    "javax.servlet"            % "javax.servlet-api" % "3.1.0" % "provided",
    "org.mitre.dsmiley.httpproxy" % "smiley-http-proxy-servlet" % "1.10",
    "com.openhtmltopdf"        % "openhtmltopdf-pdfbox" % "1.0.9",
    "org.jsoup"                % "jsoup"             % "1.12.1",
    "com.github.spullara.mustache.java" % "compiler" % "0.9.6",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.3",
    "com.norbitltd" %% "spoiwo" % "2.2.1",
    "io.github.cquiroz" %% "scala-java-time" % "2.0.0",
    "com.nrinaudo" %% "kantan.csv" % versions.kantan,
    "org.wvlet.airframe" %%% "airframe" % versions.airframe,
    "org.apache.tika" % "tika-core" % "1.25",
    "com.sksamuel.scrimage" % "scrimage-core" % "4.0.12"  exclude("ch.qos.logback","logback-classic"),
    "org.graalvm.js" % "js" % "20.2.0",
    "org.javadelight" % "delight-graaljs-sandbox" % "0.1.2",
    "org.scalatest" %% "scalatest" % versions.scalatest % "test",
    "org.scalatest" %% "scalatest-flatspec" % versions.scalatest % "test",
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test,
    "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.62.2" % Test,
    "com.github.daddykotex" %% "courier" % "3.0.0-M3a",
    "org.geotools" % "gt-shapefile" % "27.2",
    "org.geotools" % "gt-epsg-hsql" % "27.2",
    "org.geotools" % "gt-geopkg" % "27.2",
    "com.google.zxing" % "core" % "3.5.0",
    "com.google.zxing" % "javase" % "3.5.0",
    "com.typesafe" %% "ssl-config-core" % "0.6.1",
    "org.apache.xmlgraphics" % "batik-transcoder" % "1.16",
    "org.apache.xmlgraphics" % "batik-codec" % "1.16",
    "com.softwaremill.sttp.client4" %% "core" % "4.0.9",
    "com.softwaremill.sttp.client4" %% "circe" % "4.0.9"

    //"mil.nga.geopackage" % "geopackage" % "6.6.3"

    //    "com.github.pureconfig" %% "pureconfig" % "0.17.3"
    //    "com.github.andyglow" %% "scala-jsonschema" % versions.scalaJsonSchema,
//    "com.github.andyglow" %% "scala-jsonschema-circe-json" % versions.scalaJsonSchema
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "io.udash" %%% "udash-core" % versions.udash,
    "io.udash" %%% "udash-rpc" % versions.udash,
    "io.udash" %%% "udash-bootstrap4" % versions.udash,
    "io.udash" %%% "udash-jquery" % versions.udashJQuery,
    "com.github.japgolly.scalacss" %%% "core" % versions.scalaCss,
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % versions.scalaCss,
    "io.circe" %%% "circe-scalajs" % versions.circe,
    "org.scala-js" %%% "scalajs-dom" % "2.4.0",
    "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.6.0",
    "org.wvlet.airframe" %%% "airframe" % versions.airframe,
    "org.scalatest" %%% "scalatest" % versions.scalatest % Test,
    "org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0",
    "org.http4s" %%% "http4s-dom" % "0.2.3",
    "org.http4s" %%% "http4s-client" % "0.23.16",
    "org.http4s" %%% "http4s-circe" % "0.23.16",
    //"io.github.cquiroz" %%% "scala-java-locales" % "1.5.1",
//    "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.5.0"
  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
    //"org.webjars" % "bootstrap-sass" % versions.bootstrap / "3.3.1/javascripts/bootstrap.js" dependsOn "jquery.js"
  ))
}
