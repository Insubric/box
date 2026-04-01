//package ch.wsl.box.rest.metadata
//
//import ch.wsl.box.model.shared.{CurrentUser, JSONField, JSONMetadata}
//import ch.wsl.box.services.Services
//import slick.dbio.DBIO
//
//import java.util.UUID
//import scala.collection.mutable.HashMap
//import scala.concurrent.ExecutionContext
//
//class InMemoryMetadataFactory(mf:MetadataFactory) {
//
//  private val metadataByName:HashMap[(String,String),JSONMetadata] = HashMap()
//  private val metadataByUUID:HashMap[(UUID,String),JSONMetadata] = HashMap()
//
//  def load()(implicit ec: ExecutionContext, services: Services):DBIO[Boolean] = ???
//
//  private def filterRoles(user:CurrentUser)(metadata:DBIO[JSONMetadata])(implicit ec:ExecutionContext):DBIO[JSONMetadata] = metadata.map{ m =>
//
//    def filterRole(field:JSONField):Boolean = field.roles.isEmpty || field.roles.intersect(user.roles ++ Seq(user.username)).nonEmpty
//
//    m.copy(fields = m.fields.filter(filterRole))
//  }
//
//  def of(name: String, lang: String, user: CurrentUser)(implicit ec: ExecutionContext): DBIO[JSONMetadata] = filterRoles(user){
//    metadataByName.get((name, lang)) match {
//      case Some(value) => DBIO.successful(value)
//      case None => DBIO.failed(new Exception("Metadata not found"))
//    }
//  }
//
//  def of(id: UUID, lang: String, user: CurrentUser)(implicit ec: ExecutionContext, services: Services): DBIO[JSONMetadata] = filterRoles(user){
//    metadataByUUID.get((id, lang)) match {
//      case Some(value) => DBIO.successful(value)
//      case None => DBIO.failed(new Exception("Metadata not found"))
//    }
//  }
//
//  def children(form:JSONMetadata, user: CurrentUser,ignoreChilds:Seq[UUID] = Seq())(implicit ec:ExecutionContext,services:Services):DBIO[Seq[JSONMetadata]] = {
//    val childIds: Seq[UUID] = form.fields.flatMap(_.child).map(_.objId)
//
//    for{
//      firstLevel <- DBIO.sequence{childIds.map{ objId => of(objId,form.lang,user)}}
//      secondLevel <- DBIO.sequence(firstLevel.map(y => children(y,user,ignoreChilds ++ childIds)))
//    } yield {
//      firstLevel ++ secondLevel.flatten
//    }
//
//  }
//
//  def list(implicit ec: ExecutionContext, services: Services): DBIO[Seq[String]] = ???
//}
