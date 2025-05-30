import com.jsuereth.sbtpgp.PgpKeys.publishSigned
//import xerial.sbt.Sonatype.sonatypeCentralHost
import locales.LocalesFilter
import org.scalajs.jsenv.Input.Script
import scalajsbundler.util.JSON

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
    Developer(id="minettiandrea", name="Andrea Minetti", email="andrea.minetti@wsl.ch", url=url("https://wavein.ch")),
    Developer(id="pezzacolori", name="Gianni Boris Pezzatti",email="",url=url("https://github.com/pezzacolori"))
  ),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  git.gitTagToVersionNumber := { tag:String =>
    Some(tag.stripPrefix("v"))
  },

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
  publishTo := sonatypeCentralPublishToBundle.value
).settings(publishSettings).dependsOn(sharedJVM).dependsOn(serverServices)

lazy val serverServices  = (project in file("server-services")).settings(
  name := "box-server-services",
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala213,
  libraryDependencies += "com.iheart" %% "ficus" % Settings.versions.ficus,
  resolvers += Resolver.jcenterRepo,
  publishTo := sonatypeCentralPublishToBundle.value
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
    excludeDependencies ++= Seq(
      ExclusionRule(organization = "javax.media", name = "jai_core")
    ),
//    resolvers += "Eclipse" at "https://repo.eclipse.org/content/groups/snapshots",
    migrate := (Compile / runMain).toTask(" ch.wsl.box.model.Migrate").value, // register manual sbt command
    slick := slickCodeGenTask.value , // register manual sbt command
    slickTest := slickTestCodeGenTask.value , // register manual sbt command
    deleteSlick := deleteSlickTask.value,
    Compile / packageBin / mainClass := Some("ch.wsl.box.rest.Boot"),
    Compile / run / mainClass := Some("ch.wsl.box.rest.Boot"),
    Compile / resourceDirectory := baseDirectory.value / "../resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "../db",
    Test / unmanagedSourceDirectories += baseDirectory.value / "../db",
    Test / fork := true,
    Test / parallelExecution := false,
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
      if (sys.env.contains("DEV_SERVER") || sys.env.contains("RUNNING_TEST")) Seq() else Seq(client)
    },
//    scalaJSProjects := Seq(client),
    webpackBundlingMode := BundlingMode.Application,
    Seq("jquery","ol","bootstrap","flatpickr","quill","@fontsource/open-sans","@fortawesome/fontawesome-free","choices.js","gridstack","jspreadsheet-ce","jsuites","toolcool-range-slider").map{ p =>
      if (!sys.env.contains("RUNNING_TEST"))
        npmAssets ++= NpmAssets.ofProject(client) { nodeModules =>
          (nodeModules / p).allPaths
        }.value
      else npmAssets := Seq()
    },
    Test / testOptions ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-oNDXEHLO")
    ),
      publishTo := sonatypeCentralPublishToBundle.value
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
  publishTo := sonatypeCentralPublishToBundle.value
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
      "ol" -> "8.1.0",
      "proj4" -> "2.9.1",
      "@types/proj4" -> "2.5.3",
      "ol-ext" -> "4.0.11",
      //"@siedlerchr/types-ol-ext" -> "3.2.4",
      "jsts" -> "2.7.1",
      "@types/jsts" -> "0.17.13",
      "jquery" -> "3.4.1",
      "@types/jquery" -> "3.5.6",
      "popper.js" -> "1.16.1",
      "bootstrap" -> "4.1.3",
      "@types/bootstrap" -> "4.1.3",
      "@fortawesome/fontawesome-free" -> "5.15.4",
      "flatpickr" -> "4.6.3",
      "monaco-editor" -> "0.34.0",
      "quill" -> "1.3.7",
      "@types/quill" -> "1.3.10",
      "@fontsource/open-sans" -> "5.0.15",
      "file-saver" -> "2.0.5",
      "@types/file-saver" -> "2.0.1",
      "js-md5" -> "0.7.3",
      "@types/js-md5" -> "0.4.2",
      "striptags" -> "3.2.0",
      "toolcool-range-slider" -> "4.0.28",
      "hotkeys-js" -> "3.10.0",
      "crypto-browserify" -> "3.12.0",
      "buffer" -> "6.0.3",
      "stream-browserify" -> "3.0.0",
      "choices.js" -> "10.2.0",
      "autocompleter" -> "7.0.1",
      "xlsx-js-style" -> "1.2.0",
      "jspdf" -> "2.5.1",
      "jspdf-autotable" -> "3.5.28",
      "gridstack" -> "8.3.0",
      "jspreadsheet-ce" -> "git://github.com/jspreadsheet/ce.git#2e7389f8f6a84d260603bbac06f00bb404e1ba49", //v5.0.0
      "jsuites" -> "5.9.1",
      "@electric-sql/pglite" -> "0.2.17",
      "shapefile" -> "0.6.6",
      "@types/shapefile" -> "0.6.4",
    ),
    stIgnore += "@fontsource/open-sans",
    stIgnore += "redux",
    stIgnore += "node",
    stIgnore += "crypto-browserify",
    stIgnore += "ol-ext",
    stIgnore += "@fortawesome/fontawesome-free",
    stIgnore += "stream-browserify",
    stIgnore += "toolcool-range-slider",
    stTypescriptVersion := "4.2.4",
    stOutputPackage := "ch.wsl.typings",
    // Use library mode for fastOptJS
    //Compile / additionalNpmConfig := Map("sideEffects" -> JSON.bool(false)),
    fastOptJS / webpackBundlingMode := BundlingMode.Application,
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / ".." / "dev.config.js"),
    // Use application model mode for fullOptJS
    fullOptJS / webpackBundlingMode := BundlingMode.Application,
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / ".." / "prod.config.js"),
    Test / webpackConfigFile  := Some(baseDirectory.value / ".." / "test.config.js"),
    Compile / npmDevDependencies ++= Seq(
      "html-webpack-plugin" -> "5.5.0",
      "webpack-merge" -> "5.8.0",
      "style-loader" -> "3.3.1",
      "css-loader" -> "6.7.1",
      "mini-css-extract-plugin" -> "2.6.1",
      "monaco-editor-webpack-plugin" -> "7.0.1",
      "file-loader" -> "6.2.0",
    ),

    webpack / version := "5.89.0",
    webpackCliVersion := "5.1.4",
    installJsdom / version := "20.0.0",

    //To use jsdom headless browser uncomment the following lines
    Test / requireJsDomEnv := true,
    Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    Test / parallelExecution := false,
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
    localesFilter := LocalesFilter.Selection("en", "de", "fr", "it"),
    publishTo := sonatypeCentralPublishToBundle.value
  )
  .settings(publishSettings)
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterGenSourcePlugin,
    LocalesPlugin
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
    publishTo := sonatypeCentralPublishToBundle.value
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


lazy val migrate = taskKey[Unit]("migrate")


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

lazy val slickTest = taskKey[Seq[File]]("gen-test-tables")
lazy val slickTestCodeGenTask = Def.task{
  val dir = sourceDirectory.value
  val cp = (Compile / dependencyClasspath).value
  val fcp = (Compile / fullClasspath).value
  println()
  val s = streams.value
  val outputDir = (dir / "test" / "scala").getPath // place generated files in sbt's managed sources folder
  println(outputDir)
  runner.value.run("ch.wsl.box.model.TestCodeGenerator", cp.files ++ fcp.files, Array(outputDir), s.log).failed foreach (sys error _.getMessage)
  val fname = outputDir + "/ch/wsl/box/testmodel/Entities.scala"
  val ffname = outputDir + "/ch/wsl/box/testmodel/FileTables.scala"
  val rname = outputDir + "/ch/wsl/box/testmodel/GeneratedRoutes.scala"
  val registryname = outputDir + "/ch/wsl/box/testmodel/EntityActionsRegistry.scala"
  val filename = outputDir + "/ch/wsl/box/testmodel/FileRoutes.scala"
  val regname = outputDir + "/ch/wsl/box/testmodel/GenRegistry.scala"
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
    (serverServices / publishSigned)
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

