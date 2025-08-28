package ch.wsl.box.client
import ch.wsl.box.client.services.{ClientSession, DataAccessObject, HttpClient, Navigator, REST}
import ch.wsl.box.client.services.impl.{DaoLocalDbImpl, DaoPassthroughImpl, HttpClientImpl, RestImpl}
import ch.wsl.box.model.shared.AvailableUIModule
import wvlet.airframe._

object Module {

  def byName(name:String) = name match {
    case AvailableUIModule.prodNoLocalDb => prodNoLocalDb
    case AvailableUIModule.prod => prod
  }

  val prodNoLocalDb = newDesign
    .bind[HttpClient].to[HttpClientImpl]
    .bind[REST].to[RestImpl]
    .bind[DataAccessObject].to[DaoPassthroughImpl]
    .bind[ClientSession].toEagerSingleton
    .bind[Navigator].toEagerSingleton

  val prod = newDesign
    .bind[HttpClient].to[HttpClientImpl]
    .bind[REST].to[RestImpl]
    .bind[DataAccessObject].to[DaoLocalDbImpl]
    .bind[ClientSession].toEagerSingleton
    .bind[Navigator].toEagerSingleton
}
