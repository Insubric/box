package ch.wsl.box.client.services

import ch.wsl.box.client.Context
import ch.wsl.box.model.shared.{JSONMetadata, JSONQuery}
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssStyleName
import io.udash.properties.HasModelPropertyCreator
import org.scalajs.dom.Event
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by andre on 5/24/2017.
  */


case class Navigation(hasNext:Boolean, hasPrevious:Boolean, count:Int, currentIndex:Int,    //currentIndex is 1-based
                      hasNextPage:Boolean, hasPreviousPage:Boolean, pages:Int, currentPage:Int, pageLength:Int,
                      pageIDs:Seq[String]){

//  lazy val indexInPage = {                       //UDASH complains that the model is not immutable with those methods ?????
//    val i = currentIndex % pageLength
//    if (i==0) pageLength else i
//  }
//  lazy val maxIndexLastPage = count % pageLength

//  def maxIndexLge = currentIndex % pageLength   //UDASH complains that the model is not immutable with those methods ?????
//  def maxIndexLastPage = count % pageLength
}

object Navigation extends HasModelPropertyCreator[Navigation] {
  def empty0 = Navigation(false,false,0,0, false, false, 0,0,0, Seq())
  def empty1 = Navigation(false,false,1,1, false, false, 1,1,1, Seq())
  def indexInPage(nav:Navigation,currentIndex:Int) = {                //1-based
      val i = currentIndex % nav.pageLength
      if (i==0) nav.pageLength else i
    }
  def maxIndexLastPage(nav:Navigation) = (nav.count -1) % nav.pageLength + 1  //1-based

  import io.udash._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  def button(navProp:ReadableProperty[Boolean],callback: () => Unit,label:Modifier) = scalatags.JsDom.all.button(
    disabled.attrIfNot(navProp),
    ClientConf.style.boxButton,
    onclick :+= ((ev: Event) => {
      callback()
      ev.preventDefault()
    }),
    label
  )

  def pageCount(recordCount:Int) = math.ceil(recordCount.toDouble / ClientConf.pageLength.toDouble).toInt
}


class Navigator(session:ClientSession,rest:REST) extends Logging {

  import Context._

  case class For(currentId: Option[String], metadata:JSONMetadata) {

    def navigation(): Option[Navigation] = {

      val q = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery())
      val index = for{
        id <- currentId
        idx <- session.getIDs().flatMap(_.ids.zipWithIndex.find(_._1 == id).map(_._2))
      } yield idx

      logger.info(s"Navigation for $currentId, $index,\n${session.getIDs()}\n$q")

      for {
        ids <- session.getIDs()
        query <- q
        indexInPage_0based <- index
      } yield {

        Navigation(
          hasNext = !(indexInPage_0based == ids.ids.size - 1 && ids.isLastPage),
          hasPrevious = !(indexInPage_0based == 0 && query.currentPage == 1),
          count = ids.count,
          currentIndex = (query.currentPage - 1) * query.pageLength(ids.ids.size) + indexInPage_0based + 1,
          hasNextPage = !ids.isLastPage,
          hasPreviousPage = ids.currentPage > 1,
          pages = Navigation.pageCount(ids.count),
          currentPage = query.currentPage,
          pageLength = query.paging.map(_.pageLength).getOrElse(ids.ids.size),
          pageIDs = ids.ids
        )
      }
    }


    private def fetchIds(query:JSONQuery,head:Boolean)(implicit ex:ExecutionContext) = {
      services.rest.ids(metadata.kind,session.lang(),metadata.name,query).map { ids =>
        session.setQueryFor(metadata.kind,metadata.name,session.getURLQuery(),query)
        session.setIDs(ids)
        if(head) {
          ids.ids.headOption
        } else {
          ids.ids.lastOption
        }
      }
    }

    def hasNext(): Boolean = navigation().map(_.hasNext).getOrElse(false)

    def hasPrevious(): Boolean = navigation().map(_.hasPrevious).getOrElse(false)

    def hasNextPage(): Boolean = navigation().map(_.hasNextPage).getOrElse(false)

    def hasPreviousPage(): Boolean = navigation().map(_.hasPreviousPage).getOrElse(false)

    def next()(implicit ex:ExecutionContext): Future[Option[String]] = navigation.map{nav =>
      logger.info(s"Next record - current record: ${nav.currentIndex}")
      (hasNext(), Navigation.indexInPage(nav,nav.currentIndex)) match {
        case (false, _) => Future.successful(None)
        case (true, nav.pageLength) => nextPage()
        case (true, i) => Future.successful(nav.pageIDs.lift(i + 1 - 1))
      }}.getOrElse(Future.successful(None))

    def previous()(implicit ex:ExecutionContext): Future[Option[String]] = navigation.map(nav =>
      (hasPrevious(), Navigation.indexInPage(nav,nav.currentIndex)) match {
        case (false, _) => Future.successful(None)
        case (true, 1) => prevPage(false)
        case (true, i) => Future.successful(nav.pageIDs.lift(i  -1 - 1))
      }).getOrElse(Future.successful(None))

    def first()(implicit ex:ExecutionContext): Future[Option[String]] = navigation.map(nav =>
      (hasPrevious(), hasPreviousPage()) match {
        case (false, _) => Future.successful(None)
        case (true, true) => firstPage()
        case (true, false) => Future.successful(session.getIDs().get.ids.lift(1 - 1))
      }).getOrElse(Future.successful(None))

    def last()(implicit ex:ExecutionContext): Future[Option[String]] = navigation.map(nav =>
      (hasNext(), hasNextPage()) match {
        case (false, _) => Future.successful(None)
        case (true, true) => {
          val newQuery: JSONQuery = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery()).map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = navigation().map(_.pages).getOrElse(0))))).getOrElse(JSONQuery.empty)
          fetchIds(newQuery,false)
        }
        case (true, false) => Future.successful(nav.pageIDs.lift(Navigation.maxIndexLastPage(nav) - 1))
      }).getOrElse(Future.successful(None))


    def firstPage()(implicit ex:ExecutionContext): Future[Option[String]] =
      if (!hasPreviousPage()) {
        Future.successful(None)
      } else {
        val newQuery: JSONQuery = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery()).map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = 1)))).getOrElse(JSONQuery.empty)
        fetchIds(newQuery,true)
      }

    def lastPage()(implicit ex:ExecutionContext): Future[Option[String]] =
      if (!hasNextPage()) {
        Future.successful(None)
      } else {
        val newQuery: JSONQuery = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery()).map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = navigation().map(_.pages).getOrElse(0))))).getOrElse(JSONQuery.empty)
        fetchIds(newQuery,true)
      }

    def prevPage(head:Boolean = true)(implicit ex:ExecutionContext): Future[Option[String]] =
      if (!hasPreviousPage()) {
        Future.successful(None)
      } else {
        val newQuery: JSONQuery = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery()).map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = p.currentPage - 1)))).getOrElse(JSONQuery.empty)
        fetchIds(newQuery,head)
      }

    def nextPage()(implicit ex:ExecutionContext): Future[Option[String]] =
      if (!hasNextPage()) {
        Future.successful(None)
      } else {
        val newQuery: JSONQuery = session.getQueryFor(metadata.kind,metadata.name,session.getURLQuery()).map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = p.currentPage + 1)))).getOrElse(JSONQuery.empty)
        fetchIds(newQuery,true)
      }


  }

}
