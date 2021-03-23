import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream}
import java.util
import java.util.zip.GZIPOutputStream

import org.openqa.selenium.{By, Capabilities, JavascriptExecutor, WebDriver, WebElement}
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.remote.server.{DriverFactory, DriverProvider}
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import sbt.URL

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

object BrowserStackRunner {

  def load(): SeleniumJSEnv = {

    val AUTOMATE_USERNAME = System.getenv("AUTOMATE_USERNAME")
    val AUTOMATE_ACCESS_KEY = System.getenv("AUTOMATE_ACCESS_KEY")
    val URL = "https://" + AUTOMATE_USERNAME + ":" + AUTOMATE_ACCESS_KEY + "@hub-cloud.browserstack.com/wd/hub"


    import com.browserstack.local.Local

    val tmpDir = new java.io.File(".tmp")

    Try {
      val bsLocal = new Local()


      if (!tmpDir.exists) tmpDir.mkdir()

      val bsLocalArgs = new java.util.HashMap[String, String]()
      bsLocalArgs.put("key", AUTOMATE_ACCESS_KEY)
      println(tmpDir.getAbsolutePath)
      bsLocalArgs.put("folder", tmpDir.getAbsolutePath + "/")
      bsLocal.start(bsLocalArgs);

      println(bsLocal.isRunning());
    }

    new SimpleHttpServer(tmpDir).start()



    val caps = new DesiredCapabilities
    caps.setCapability("os", "Windows")
    caps.setCapability("os_version", "10")
    caps.setCapability("resolution", "1920x1080")
    caps.setCapability("browser", "Chrome")
    caps.setCapability("browser_version", "latest")
    caps.setCapability("browserstack.selenium_version", "3.141.59")
    caps.setCapability("browserstack.local", "true")
    caps.setCapability("browserstack.networkLogs", "true")
    caps.setCapability("browserstack.console", "info")
    caps.setCapability("name", "Box Framework Test") // test name
    caps.setCapability("build", Try(System.getenv("BUILD_CODE")).getOrElse("Build code")) // CI/CD job or build name


    val jsenv = new org.scalajs.jsenv.selenium.SeleniumJSEnv(caps, SeleniumJSEnv.Config()
      .withMaterializeInServer(".tmp", "http://localhost:3000/")
      .withDriverFactory(new DriverFactory {
        override def registerDriverProvider(driverProvider: DriverProvider): Unit = {}

        override def newInstance(capabilities: Capabilities) = {

          val d = new RemoteWebDriver(new URL(URL), capabilities)

          new WebDriver with JavascriptExecutor{
            override def get(s: String): Unit = d.get(s)
            override def getCurrentUrl: String = d.getCurrentUrl
            override def getTitle: String = d.getTitle
            override def findElements(by: By): util.List[WebElement] = d.findElements(by)
            override def findElement(by: By): WebElement = d.findElement(by)
            override def getPageSource: String = d.getPageSource
            override def close(): Unit = {
              println("calling close")
              //d.close()
              d.quit()
            }
            override def quit(): Unit = d.quit()
            override def getWindowHandles: util.Set[String] = d.getWindowHandles
            override def getWindowHandle: String = d.getWindowHandle
            override def switchTo(): WebDriver.TargetLocator = d.switchTo()
            override def navigate(): WebDriver.Navigation = d.navigate()
            override def manage(): WebDriver.Options = d.manage()
            override def executeScript(s: String, objects: Object*): Object = d.executeScript(s, objects: _*)
            override def executeAsyncScript(s: String, objects: Object*): Object = d.executeAsyncScript(s,objects:_*)
          }


        }
      })
      .withKeepAlive(false)

    )
    println(jsenv.name)
    jsenv
  }

}

import java.io.{InputStream, OutputStream}
import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

class SimpleHttpServer(root:File) extends Thread {

  override def run() {
    val server = HttpServer.create(new InetSocketAddress(3000), 0)
    server.createContext("/", new RootHandler(root))
    server.setExecutor(null)

    server.start()

  }

}

class RootHandler(root:File) extends HttpHandler {

  def handle(t: HttpExchange) {
    //displayPayload(t.getRequestBody)
    sendResponse(t)
  }

  private def displayPayload(body: InputStream): Unit ={
    println()
    println("******************** REQUEST START ********************")
    println()
    copyStream(body, System.out)
    println()
    println("********************* REQUEST END *********************")
    println()
  }

  private def copyStream(in: InputStream, out: OutputStream) {
    Iterator
      .continually(in.read)
      .takeWhile(-1 !=)
      .foreach(out.write)
  }

  private def sendResponse(t: HttpExchange) {
    val uri = t.getRequestURI();
    println("looking for: " + root.getAbsolutePath + uri.getPath());
    val path = uri.getPath();
    val file = new File(root.getAbsolutePath + path)

    if (!file.exists()) {
      // Object does not exist or is not a file: reject with 404 error.
      val response = "404 (Not Found)\n";
      t.sendResponseHeaders(404, response.length());
      val os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else {
      // Object exists and is a file: accept with response code 200.
      val mime: String =
        if (path.substring(path.length() - 3).equals(".js")) "application/javascript"
        else if (path.substring(path.length() - 3).equals("css")) "text/css"
        else "text/html"


      // compress
      val bufferGzip = Array.ofDim[Byte](4096)
      val bout = new ByteArrayOutputStream();
      val gout = new GZIPOutputStream(bout)
      val fs = new FileInputStream(file)

      var countGzip = 0
      while ({countGzip = fs.read(bufferGzip); countGzip }  >= 0) {
        gout.write(bufferGzip, 0, countGzip)
      }
      gout.flush()
      gout.close()
      fs.close()
      val gzipResource = bout.toByteArray



      val h = t.getResponseHeaders()
      h.set("Content-Type", mime)
      h.set("Content-Encoding", "gzip")
      t.sendResponseHeaders(200, gzipResource.length)
      println(gzipResource.length)
      val buffer = Array.ofDim[Byte](0x10000)
      val os = t.getResponseBody()
      val bais = new ByteArrayInputStream(gzipResource)

      var count = 0
      while ({count = bais.read(buffer); count } >= 0) {
        os.write(buffer, 0, count)
      }
      bais.close()
      os.close()



    }
  }

}
