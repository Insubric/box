package ch.wsl.box.client
import ch.wsl.box.client.services.{ClientSession, DataAccessObject, HttpClient, Navigator, REST}
import ch.wsl.box.client.services.impl.{DaoImpl, HttpClientImpl, RestImpl}
import wvlet.airframe._

object Module {
  val prod = newDesign
    .bind[HttpClient].to[HttpClientImpl]
    .bind[REST].to[RestImpl]
    .bind[DataAccessObject].to[DaoImpl]
    .bind[ClientSession].toEagerSingleton
    .bind[Navigator].toEagerSingleton
}
