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
  private val webJarAssetLocator = new WebJarAssetLocator

  def webJars = {
    extractUnmatchedPath { path =>
      val webjarName = path.toString().substring(1)
      logger.info("Looking for webjar: " + webjarName )
      Try(webJarAssetLocator.getFullPath(webjarName)) match {
        case Success(fullPath) => {
          logger.info("found")
//          if(fullPath.endsWith("woff2")) {
//            getFromResource(fullPath,contentType = ContentType.apply(MediaType.applicationBinary("font-woff2",MediaType.NotCompressible)))
//          } else {
            getFromResource(fullPath)
//          }
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

  def bundle = {
    extractUnmatchedPath { path =>
      val webjarName = path.toString().substring(1)
      logger.info("Looking for webjar: " + webjarName )
      Try(webJarAssetLocator.getFullPathExact("box-server",webjarName)) match {
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
