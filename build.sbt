import org.scalajs.jsenv.Input.Script

/** codegen project containing the customized code generator */
lazy val codegen  = (project in file("codegen")).settings(
  organization := "boxframework",
  name := "box-codegen",
  bintrayRepository := "maven",
  bintrayOrganization := Some("waveinch"),
  publishMavenStyle := true,
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala212,
  libraryDependencies ++= Settings.codegenDependecies.value,
  resolvers += Resolver.jcenterRepo,
  resolvers += Resolver.bintrayRepo("waveinch","maven"),
  resourceDirectory in Compile := baseDirectory.value / "../resources",
  unmanagedResourceDirectories in Compile += baseDirectory.value / "../db",
  git.useGitDescribe := true
).dependsOn(sharedJVM)

lazy val serverServices  = (project in file("server-services")).settings(
  organization := "boxframework",
  name := "box-server-services",
  bintrayRepository := "maven",
  bintrayOrganization := Some("waveinch"),
  publishMavenStyle := true,
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala212,
  libraryDependencies ++= Settings.serverCacheRedisDependecies.value,
  resolvers += Resolver.jcenterRepo,
  resolvers += Resolver.bintrayRepo("waveinch","maven"),
  git.useGitDescribe := true
).dependsOn(sharedJVM)

lazy val server: Project  = project
  .settings(
    organization := "boxframework",
    name := "box-server",
    bintrayRepository := "maven",
    bintrayOrganization := Some("waveinch"),
    publishMavenStyle := true,
    licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
    scalaVersion := Settings.versions.scala212,
    scalaBinaryVersion := "2.12",
    scalacOptions ++= Settings.scalacOptionsServer,
    libraryDependencies ++= Settings.jvmDependencies.value,
    resolvers ++= Seq(Resolver.jcenterRepo, Resolver.bintrayRepo("hseeberger", "maven")),
    resolvers += Resolver.bintrayRepo("waveinch","maven"),
    resolvers += "OSGeo Releases" at "https://repo.osgeo.org/repository/release",
    slick := slickCodeGenTask.value , // register manual sbt command
    deleteSlick := deleteSlickTask.value,
    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "html"),
    mainClass in (Compile, packageBin) := Some("ch.wsl.box.rest.Boot"),
    mainClass in (Compile, run) := Some("ch.wsl.box.rest.Boot"),
    resourceDirectory in Compile := baseDirectory.value / "../resources",
    unmanagedResourceDirectories in Test += baseDirectory.value / "../db",
    fork in Test := true,
    git.useGitDescribe := true,
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "boxInfo",
    buildInfoObject := "BoxBuildInfo",
    pipelineStages in Assets := Seq(scalaJSPipeline),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    //Comment this to avoid errors in importing project, i.e. when changing libraries
    pipelineStages in Assets := Seq(scalaJSPipeline),
    scalaJSProjects := Seq(client),
    Seq("jquery","ol","bootstrap","flatpickr","quill","@fontsource/open-sans").map{ p =>
      npmAssets ++= NpmAssets.ofProject(client) { nodeModules =>
        (nodeModules / p).allPaths
      }.value
    },
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-oNDXEHLO")
    )
  )
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
  organization := "boxframework",
  name := "box-server-cache-redis",
  bintrayRepository := "maven",
  bintrayOrganization := Some("waveinch"),
  publishMavenStyle := true,
  licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
  scalaVersion := Settings.versions.scala212,
  libraryDependencies ++= Settings.serverCacheRedisDependecies.value,
  resolvers += Resolver.jcenterRepo,
  resolvers += Resolver.bintrayRepo("waveinch","maven"),
  git.useGitDescribe := true
).dependsOn(serverServices)

lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    scalaVersion := Settings.versions.scala213,
    scalacOptions ++= Settings.scalacOptions,
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.bintrayRepo("waveinch","maven"),
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    Compile / npmDependencies ++= Seq(
      "ol" -> "6.3.1",
      "@types/ol" -> "6.3.1",
      "proj4" -> "2.5.0",
      "@types/proj4" -> "2.5.0",
      "ol-ext" -> "3.1.14",
      "jquery" -> "3.3.1",
      "popper.js" -> "1.16.1",
      "@types/jquery" -> "3.3.1",
      "bootstrap" -> "4.1.3",
      "@types/bootstrap" -> "4.1.3",
      "flatpickr" -> "4.6.3",
      "monaco-editor" -> "0.21.1",
      "quill" -> "1.3.7",
      "@types/quill" -> "1.3.10",
      "@fontsource/open-sans" -> "4.2.1",
      "file-saver" -> "2.0.5",
      "@types/file-saver" -> "2.0.1",
      "js-md5" -> "0.7.3",
      "@types/js-md5" -> "0.4.2",
      "print-js" -> "1.6.0"
    ),
    stIgnore += "@fontsource/open-sans",
    stIgnore += "ol-ext",
    // Use library mode for fastOptJS
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(),
    webpackConfigFile in fastOptJS := Some(baseDirectory.value / ".." / "dev.config.js"),
    // Use application model mode for fullOptJS
    webpackBundlingMode in fullOptJS := BundlingMode.Application,
    webpackConfigFile in fullOptJS := Some(baseDirectory.value / ".." / "prod.config.js"),
    webpackConfigFile in Test := Some(baseDirectory.value / ".." / "test.config.js"),
    npmDevDependencies in Compile ++= Seq(
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
    version in webpack := "4.43.0",
    version in installJsdom := "16.4.0",
    version in startWebpackDevServer := "3.11.0",
    fork in fastOptJS := true,
    fork in fullOptJS := true,
    javaOptions in fastOptJS += "-Xmx4G -XX:MaxMetaspaceSize=1G -XX:MaxPermSize=1G -XX:+CMSClassUnloadingEnabled -Xss3m",
    javaOptions in fullOptJS += "-Xmx4G -XX:MaxMetaspaceSize=1G -XX:MaxPermSize=1G -XX:+CMSClassUnloadingEnabled -Xss3m",

    //To use jsdom headless browser uncomment the following lines
//    jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
//    jsEnvInput in Test := Def.task{
//      val targetDir = (npmUpdate in Test).value
//      println(targetDir)
//      val r = Seq(Script((targetDir / s"fixTest.js").toPath)) ++ (jsEnvInput in Test).value
//      println(r)
//      r
//    }.value,

    //To use Selenium uncomment the following line
    scalaJSStage in Test := FullOptStage,
    jsEnv in Test := BrowserStackRunner.load(),
    concurrentRestrictions := Seq(
      Tags.limit(Tags.Test,5) //browserstack limit
    ),
    requireJsDomEnv in Test := true,
  )
  .enablePlugins(
    ScalaJSPlugin,
    ScalablyTypedConverterPlugin
  )
  .dependsOn(sharedJS)





//CrossProject is a Project compiled with both java and javascript
lazy val shared = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    organization := "boxframework",
    name := "box-shared",
    bintrayRepository := "maven",
    bintrayOrganization := Some("waveinch"),
    licenses += ("Apache-2.0", url("http://www.opensource.org/licenses/apache2.0.php")),
    libraryDependencies ++= Settings.sharedJVMJSDependencies.value,
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.bintrayRepo("waveinch","maven"),
    git.useGitDescribe := true
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
  )

lazy val sharedJVM: Project = shared.jvm.settings(
  name := "box-shared-jvm",
  scalaVersion := Settings.versions.scala212,
)

lazy val sharedJS: Project = shared.js.settings(
  name := "box-shared-js",
  scalaVersion := Settings.versions.scala213,
)


// code generation task that calls the customized code generator
lazy val slick = taskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = Def.task{
  val dir = sourceDirectory.value
  val cp = (dependencyClasspath in Compile).value
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
    dropBox := dropBoxTask.value,
  )



lazy val publishAll = taskKey[Unit]("Publish all modules")
lazy val publishAllTask = {
  Def.sequential(
    (clean in client),
    (clean in server),
    (clean in codegen),
    (clean in serverCacheRedis),
    (clean in serverServices),
    (webpack in fullOptJS in Compile in client),
    (compile in Compile in codegen),
    (publish in sharedJVM),
    (publish in codegen),
    (publish in server),
    (publish in serverCacheRedis),
    (publish in serverServices),
  )
}

lazy val publishAllLocal = taskKey[Unit]("Publish all modules")
lazy val publishAllLocalTask = {
  Def.sequential(
    (clean in client),
    (clean in server),
    (clean in serverCacheRedis),
    (clean in serverServices),
    (clean in codegen),
    (webpack in fullOptJS in Compile in client),
    (compile in Compile in codegen),
    (publishLocal in sharedJVM),
    (publishLocal in codegen),
    (publishLocal in server),
    (publishLocal in serverCacheRedis),
    (publishLocal in serverServices),
  )
}

lazy val installBox = taskKey[Unit]("Install box schema")
lazy val installBoxTask = Def.sequential(
  //cleanAll,
  (compile in Compile in server).toTask,
  Def.task{
    (runMain in Compile in server).toTask(" ch.wsl.box.model.BuildBox").value
  }
)


lazy val dropBox = taskKey[Unit]("Drop box schema")
lazy val dropBoxTask = Def.sequential(
  //cleanAll,
  (compile in Compile in server).toTask,
  Def.task{
    (runMain in Compile in server).toTask(" ch.wsl.box.model.DropBox").value
  }
)




