import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import org.scalajs.jsenv.Input.Script

val publishSettings = List(
  Global / scalaJSStage := FullOptStage,
  organization := "com.boxframework",
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://www.boxframework.com/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/Insubric/box"),
      "scm:git@github.com:Insubric/box.git"
    )
  ),
  developers := List(
    Developer(id="minettiandrea", name="Andrea Minetti", email="andrea@wavein.ch", url=url("https://wavein.ch")),
    Developer(id="pezzacolori", name="Gianni Boris Pezzatti",email="",url=url("https://github.com/pezzacolori"))
  ),
  dynverSeparator := "-",
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  dynverSonatypeSnapshots := true,
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "s01.oss.sonatype.org",
    System.getenv("SONATYPE_USERNAME"),
    System.getenv("SONATYPE_PASSWORD")
  )
)

inThisBuild(publishSettings)

publish / skip := true

/** codegen project containing the customized code generator */
lazy val codegen  = (project in file("codegen")).settings(
  name := "box-codegen",
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala213,
  libraryDependencies ++= Settings.codegenDependecies.value,
  resolvers += Resolver.jcenterRepo,
  Compile / resourceDirectory := baseDirectory.value / "../resources",
  Compile / unmanagedResourceDirectories += baseDirectory.value / "../db",
).settings(publishSettings).dependsOn(sharedJVM).dependsOn(serverServices)

lazy val serverServices  = (project in file("server-services")).settings(
  name := "box-server-services",
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala213,
  libraryDependencies += "com.iheart" %% "ficus" % Settings.versions.ficus,
  resolvers += Resolver.jcenterRepo,
).settings(publishSettings).dependsOn(sharedJVM)

lazy val server: Project  = project
  .settings(
    name := "box-server",
    licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
    scalaVersion := Settings.versions.scala213,
    scalaBinaryVersion := "2.13",
    scalacOptions ++= Settings.scalacOptionsServer,
    libraryDependencies ++= Settings.jvmDependencies.value,
    resolvers += "OSGeo Releases" at "https://repo.osgeo.org/repository/release",
    resolvers += "Eclipse" at "https://repo.eclipse.org/content/groups/snapshots",
    slick := slickCodeGenTask.value , // register manual sbt command
    deleteSlick := deleteSlickTask.value,
    Compile / packageBin / mainClass := Some("ch.wsl.box.rest.Boot"),
    Compile / run / mainClass := Some("ch.wsl.box.rest.Boot"),
    Compile / resourceDirectory := baseDirectory.value / "../resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "../db",
    Test / unmanagedSourceDirectories += baseDirectory.value / "../db",
    Test / fork := true,
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "boxInfo",
    buildInfoObject := "BoxBuildInfo",
    Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
    Runtime / managedClasspath += (Assets / packageBin).value,
    Assets / WebKeys.packagePrefix := "public/",
    //Comment this to avoid errors in importing project, i.e. when changing libraries
    Assets / pipelineStages := Seq(scalaJSPipeline),
    Assets / scalaJSStage := FullOptStage,
    scalaJSProjects := {
      if (sys.env.get("DEV_SERVER").isDefined) Seq() else Seq(client)
    },
//    scalaJSProjects := Seq(client),
    webpackBundlingMode := BundlingMode.Application,
    Seq("jquery","ol","bootstrap","flatpickr","quill","open-sans-all","@fortawesome/fontawesome-free").map{ p =>
      npmAssets ++= NpmAssets.ofProject(client) { nodeModules =>
        (nodeModules / p).allPaths
      }.value
    },
    Test / testOptions ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-oNDXEHLO")
    ),
  ).settings(publishSettings)
  .enablePlugins(
    GitVersioning,
    BuildInfoPlugin,
    WebScalaJSBundlerPlugin,
    SbtTwirl
  )
  .dependsOn(sharedJVM)
  .dependsOn(codegen)
  .dependsOn(serverServices)



lazy val serverCacheRedis  = (project in file("server-cache-redis")).settings(
  name := "box-server-cache-redis",
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala213,
  libraryDependencies ++= Settings.serverCacheRedisDependecies.value,
  resolvers += Resolver.jcenterRepo,
).settings(publishSettings).dependsOn(serverServices)

lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    scalaVersion := Settings.versions.scala213,
    scalacOptions ++= Settings.scalacOptions,
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // yes, we want to package JS dependencies
    packageJSDependencies / skip := false,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    scalaJSStage := FullOptStage,
    Compile / npmDependencies ++= Seq(
      "ol" -> "6.3.1",
      "@types/ol" -> "6.3.1",
      "proj4" -> "2.5.0",
      "@types/proj4" -> "2.5.0",
      "ol-ext" -> "3.1.14",
      "jquery" -> "3.4.1",
      "@types/jquery" -> "3.5.6",
      "popper.js" -> "1.16.1",
      "bootstrap" -> "4.1.3",
      "@types/bootstrap" -> "4.1.3",
      "@fortawesome/fontawesome-free" -> "5.15.4",
      "flatpickr" -> "4.6.3",
      "monaco-editor" -> "0.21.1",
      "quill" -> "1.3.7",
      "@types/quill" -> "1.3.10",
      "open-sans-all" -> "0.1.3",
      "file-saver" -> "2.0.5",
      "@types/file-saver" -> "2.0.1",
      "js-md5" -> "0.7.3",
      "@types/js-md5" -> "0.4.2",
      "print-js" -> "1.6.0",
      "striptags" -> "3.2.0",
      "toolcool-range-slider" -> "2.0.12"
    ),
    stIgnore += "open-sans-all",
    stIgnore += "ol-ext",
    stIgnore += "@fortawesome/fontawesome-free",
    stTypescriptVersion := "4.2.4",
    // Use library mode for fastOptJS
    fastOptJS / webpackBundlingMode := BundlingMode.LibraryOnly(),
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / ".." / "dev.config.js"),
    // Use application model mode for fullOptJS
    fullOptJS / webpackBundlingMode := BundlingMode.Application,
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / ".." / "prod.config.js"),
    Test / webpackConfigFile  := Some(baseDirectory.value / ".." / "test.config.js"),
    Compile / npmDevDependencies ++= Seq(
      "html-webpack-plugin" -> "4.3.0",
      "webpack-merge" -> "4.2.2",
      "style-loader" -> "1.2.1",
      "css-loader" -> "3.5.3",
      "mini-css-extract-plugin" -> "0.9.0",
      "monaco-editor-webpack-plugin" -> "2.0.0",
      "file-loader" -> "6.1.0",
    ),
    // https://scalacenter.github.io/scalajs-bundler/cookbook.html#webpack-dev-server
    webpackDevServerPort := 8888,
    webpack / version := "4.43.0",
    installJsdom / version := "16.4.0",
    startWebpackDevServer / version  := "3.11.0",


    //To use jsdom headless browser uncomment the following lines
    Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    Test / jsEnvInput := Def.task{
      val targetDir = (npmUpdate in Test).value
      println(targetDir)
      val r = Seq(Script((targetDir / s"fixTest.js").toPath)) ++ (jsEnvInput in Test).value
      println(r)
      r
    }.value,
    Test / scalaJSStage := FastOptStage,

    //To use Selenium uncomment the following line
//    Test / scalaJSStage := FullOptStage,
//    Test / jsEnv := BrowserStackRunner.load(),

    concurrentRestrictions := Seq(
      Tags.limit(Tags.Test,5) //browserstack limit
    ),
    Test / requireJsDomEnv := true,

  )
  .settings(publishSettings)
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterPlugin
  )
  .dependsOn(sharedJS)





//CrossProject is a Project compiled with both java and javascript
lazy val shared = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    name := "box-shared",
    licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
    libraryDependencies ++= Settings.sharedJVMJSDependencies.value,
    resolvers += Resolver.jcenterRepo,
  )
  .settings(publishSettings)
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
  )

lazy val sharedJVM: Project = shared.jvm.settings(
  name := "box-shared-jvm",
  scalaVersion := Settings.versions.scala213,
)

lazy val sharedJS: Project = shared.js.settings(
  name := "box-shared-js",
  scalaVersion := Settings.versions.scala213,
)


// code generation task that calls the customized code generator
lazy val slick = taskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = Def.task{
  val dir = sourceDirectory.value
  val cp = (Compile / dependencyClasspath).value
  val s = streams.value
  val outputDir = (dir / "main" / "scala").getPath // place generated files in sbt's managed sources folder
  println(outputDir)
  runner.value.run("ch.wsl.box.codegen.CustomizedCodeGenerator", cp.files, Array(outputDir), s.log).failed foreach (sys error _.getMessage)
  val fname = outputDir + "/ch/wsl/box/generated/Entities.scala"
  val ffname = outputDir + "/ch/wsl/box/generated/FileTables.scala"
  val rname = outputDir + "/ch/wsl/box/generated/GeneratedRoutes.scala"
  val registryname = outputDir + "/ch/wsl/box/generated/EntityActionsRegistry.scala"
  val filename = outputDir + "/ch/wsl/box/generated/FileRoutes.scala"
  val regname = outputDir + "/ch/wsl/box/generated/GenRegistry.scala"
  Seq(file(fname),file(ffname),file(rname),file(registryname),file(filename),file(regname))    //include the generated files in the sbt project
}

lazy val deleteSlick = taskKey[Unit]("Delete slick generated files")
lazy val deleteSlickTask = Def.task{
  val dir = sourceDirectory.value
  val outputDir = (dir / "main" / "scala" / "ch" / "wsl" / "box" / "generated")
  IO.delete(Seq(
    outputDir
  ))
}

lazy val box = (project in file("."))
  .settings(
    publishAll := publishAllTask.value,
    publishAllLocal := publishAllLocalTask.value,
    installBox := installBoxTask.value,
    dropBox := dropBoxTask.value
  ).settings(publishSettings)



lazy val publishAll = taskKey[Unit]("Publish all modules")
lazy val publishAllTask = {
  Def.sequential(
    (client / clean),
    (server / clean),
    (serverCacheRedis / clean),
    (serverServices / clean),
    (codegen / clean),
    (client / Compile / fullOptJS / webpack),
    (client / Compile / fullOptJS),
    (codegen / Compile / compile),
    (sharedJVM / publishSigned),
    (codegen / publishSigned),
    (server / publishSigned),
    (serverCacheRedis / publishSigned),
    (serverServices / publishSigned),
  )
}


lazy val publishAllLocal = taskKey[Unit]("Publish all modules")
lazy val publishAllLocalTask = {
  Def.sequential(
    (client / Compile / fullOptJS / webpack),
    (codegen / Compile / compile),
    (sharedJVM / publishLocal),
    (codegen / publishLocal),
    (server / publishLocal),
    (serverCacheRedis / publishLocal),
    (serverServices / publishLocal),
  )
}

lazy val installBox = taskKey[Unit]("Install box schema")
lazy val installBoxTask = Def.sequential(
  //cleanAll,
  (server / Compile / compile).toTask,
  Def.task{
    (server / Compile / runMain).toTask(" ch.wsl.box.model.BuildBox").value
  }
)


lazy val dropBox = taskKey[Unit]("Drop box schema")
lazy val dropBoxTask = Def.sequential(
  //cleanAll,
  (server / Compile / compile).toTask,
  Def.task{
    (server / Compile / runMain ).toTask(" ch.wsl.box.model.DropBox").value
  }
)
