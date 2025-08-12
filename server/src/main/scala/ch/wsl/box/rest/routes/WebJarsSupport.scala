package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{ContentType, MediaType}
import akka.http.scaladsl.server.Directives._
import org.webjars.{MultipleMatchesException, WebJarAssetLocator}
import scribe.Logging

import scala.util.{Failure, Success, Try}

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object WebJarsSupport extends Logging {
  private val locator = new WebJarAssetLocator

  def fullPath(path:String, contentType: ContentType) = {
    Try(locator.getFullPath(path)) match {
      case Success(fullPath) => {
        getFromResource(fullPath,contentType = contentType)
      }
      case Failure(e) => {
        e.printStackTrace()
        failWith(e)
      }
    }
  }


  def webJars = {
    extractUnmatchedPath { path =>
      val webjarName = path.toString().substring(1)
      logger.info("Looking for webjar: " + webjarName )
      Try(locator.getFullPath(webjarName)) match {
        case Success(fullPath)  if fullPath.endsWith("wasm") => getFromResource(fullPath, contentType = ContentType.apply(MediaType.applicationBinary("wasm",MediaType.Compressible)))
        case Success(fullPath) => getFromResource(fullPath)
        case Failure(e: MultipleMatchesException) => {
          print(e.getMatches)
          e.printStackTrace()
          reject
        }
        case Failure(e: IllegalArgumentException) => {
          e.printStackTrace()
          reject
        }
        case Failure(e) => {
          e.printStackTrace()
          failWith(e)
        }
      }
    }
  }

  def bundle = {
    extractUnmatchedPath { path =>
      val webjarName = path.toString().substring(1)
      logger.info("Looking for webjar: " + webjarName )
      Try(locator.getFullPathExact("box-server",webjarName)) match {
        case Success(fullPath) => {
          logger.info("found")
          getFromResource(fullPath)
        }
        case Failure(e: MultipleMatchesException) => {
          print(e.getMatches)
          e.printStackTrace()
          reject
        }
        case Failure(e: IllegalArgumentException) => {
          e.printStackTrace()
          reject
        }
        case Failure(e) => {
          e.printStackTrace()
          failWith(e)
        }
      }
    }
  }
}
