package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.services.Record
import ch.wsl.box.model.shared.{FormAction, JSONID}
import io.circe.Json
import io.udash.ReadableProperty

import scala.concurrent.{ExecutionContext, Future}

trait ActionHandler {

  def data:ReadableProperty[Json]

  def beforeAction():Unit

  def reload()

  def reload(id:JSONID): Future[Json]

  def beforeSave(check:Boolean = true):Future[Json]

  def afterSave(id:JSONID,data:Record)

  def saveToDb(data:Json):Future[(JSONID,Record)]

  def saveLocally(data:Json):Future[(JSONID,Record)]

  def duplicate():Unit

  def delete(id:Option[JSONID],data:Json,write:Boolean,action: FormAction)

  def save(check:Boolean = true)(implicit ex:ExecutionContext):Unit = beforeSave(check).flatMap(saveToDb).foreach( x=> afterSave(x._1,x._2))

}
