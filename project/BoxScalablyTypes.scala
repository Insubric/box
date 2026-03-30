import com.olvind.logging
import com.olvind.logging.LogLevel
import org.scalablytyped.converter.{Flavour, Selection}
import org.scalablytyped.converter.internal.{BuildInfo, IArray, ImportTypingsGenSources, InFolder, Json, files}
import org.scalablytyped.converter.internal.ImportTypingsGenSources.Input
import org.scalablytyped.converter.internal.importer.{ConversionOptions, EnabledTypeMappingExpansion}
import org.scalablytyped.converter.internal.scalajs.{Name, QualifiedName, Versions}
import org.scalablytyped.converter.internal.ts.{PackageJson, TsIdentLibrary}
import os.Path
import sbt.*

import scala.collection.immutable.{SortedMap, SortedSet}

object BoxScalablyTypes {

  val outputName = Name("ch.wsl.typings")




  val conversion = ConversionOptions(
    useScalaJsDomTypes       = true,
    flavour                  = Flavour.Slinky,
    outputPackage            = outputName,
    enableScalaJsDefined     = Selection.All,
    stdLibs                  = SortedSet("es6", "es2018.asyncgenerator"),
    expandTypeMappings       = EnabledTypeMappingExpansion.DefaultSelection,
    ignored                  = SortedSet(
      "@fontsource/open-sans",
      "redux",
      "node",
      "crypto-browserify",
      "ol-ext",
      "@fortawesome/fontawesome-free",
      "stream-browserify",
      "toolcool-range-slider",
      "@tailwindcss/vite",
      "string-strip-html",
      "autocompleter",
    ),
    versions                 = Versions(Versions.Scala213, Versions.ScalaJs1),
    organization             = "org.scalablytyped",
    enableReactTreeShaking   = Selection.None,
    enableLongApplyMethod    = false,
    privateWithin            = None,
    useDeprecatedModuleNames = false,
  )



  def generateSJSFromTS(_clientPath: SettingKey[_root_.java.io.File]) = Def.task {
    val clientPath = os.Path(_clientPath.value)

    val libs = Json.force[PackageJson](clientPath / "package.json").allLibs(dev = false, peer = false)

    println(libs)

    ImportTypingsGenSources(
      input = Input(
        fromFolder = InFolder(clientPath / "node_modules"),
        targetFolder = files.existing(clientPath / "target" / "scala-2.13" / "src_scalablytypes" / "main"),
        overrideTargetFolder = Map.empty,
        converterVersion = BuildInfo.version,
        conversion = conversion,
        wantedLibs = libs,
        minimize = Selection.NoneExcept(TsIdentLibrary("std")),
        minimizeKeep = IArray(),
      ),
      logger = logging.stdout.filter(LogLevel.warn),
      parseCacheDirOpt = None,
      cacheDirOpt = clientPath,
    ).toOption.toSeq.flatten

  }



}