package ch.wsl.box.client.services.impl

import ch.wsl.box.client.Context
import ch.wsl.box.client.services.{Preferences, REST, TablePreference, UserPreferences}
import ch.wsl.box.model.shared.JSONMetadata

import scala.concurrent.{ExecutionContext, Future}

class PreferencesImpl(rest:REST) extends Preferences {

  import Context.Implicits._

  private var _preferences:Option[UserPreferences] = None

  def load():Future[Boolean] = for{
    p <- rest.preferences()
  }  yield {
    _preferences = p
    true
  }

  val defaultPreference = UserPreferences()

  def updatePreference(f: UserPreferences => UserPreferences):Future[Boolean] = {
    val p = _preferences.getOrElse(defaultPreference)
    _preferences = Some(f(p))
    rest.savePreferences(_preferences.get)
  }


  override def table(metadata: JSONMetadata): Option[TablePreference] = _preferences.flatMap(_.tables.toSeq.flatten.find(_.entity == metadata.uniqueName))

  override def saveTable(tp: TablePreference): Future[Boolean] = updatePreference{ p =>
    p.copy(tables = Some(p.tables.toSeq.flatten.filterNot(_.entity == tp.entity) ++ Seq(tp)))
  }
}
