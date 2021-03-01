package ch.wsl.box.rest.io

import java.io.{File, FileInputStream, InputStream}
import java.util

import org.geotools.data.shapefile.ShapefileDumper

object ShapeFileWriter {





  val shpFile = new File("./tmp")
  val dumper = new ShapefileDumper(shpFile)
  //dumper.dump()





}
