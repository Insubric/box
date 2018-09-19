package ch.wsl.box.client.utils

import ch.wsl.box.client.services.REST
import ch.wsl.box.model.shared.{IDs, JSONQuery}

import scala.concurrent.Future
import ch.wsl.box.client.Context._

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

object Navigation{
  def empty0 = Navigation(false,false,0,0, false, false, 0,0,0, Seq())
  def empty1 = Navigation(false,false,1,1, false, false, 1,1,1, Seq())
  def indexInPage(nav:Navigation) = {                //1-based
      val i = nav.currentIndex % nav.pageLength
      if (i==0) nav.pageLength else i
    }
  def maxIndexLastPage(nav:Navigation) = nav.count % nav.pageLength   //1-based
}



case class Navigator(currentId:Option[String], kind:String = "", model:String = "") {

  def navigation():Option[Navigation] = for {
    id <- currentId
    ids <- Session.getIDs()
    query <- Session.getQuery()
    (_, indexInPage_0based) <- ids.ids.zipWithIndex.find(_._1 == id)
  } yield {

    Navigation(
      hasNext = !(indexInPage_0based == ids.ids.size-1 && ids.isLastPage),
      hasPrevious =  !(indexInPage_0based == 0 && query.currentPage == 1),
      count = ids.count,
      currentIndex = (query.currentPage-1)*query.pageLength(ids.ids.size) + indexInPage_0based + 1,
      hasNextPage = !ids.isLastPage,
      hasPreviousPage = ids.currentPage>1,
      pages = ids.count / query.pageLength(ids.ids.size) +1 ,
      currentPage = query.currentPage,
      pageLength = query.paging.map(_.pageLength).getOrElse(ids.ids.size),
      pageIDs = ids.ids
    )
  }


  def hasNext():Boolean = navigation().map(_.hasNext).getOrElse(false)

  def hasPrevious():Boolean = navigation().map(_.hasPrevious).getOrElse(false)

  def hasNextPage():Boolean = navigation().map(_.hasNextPage).getOrElse(false)

  def hasPreviousPage():Boolean = navigation().map(_.hasPreviousPage).getOrElse(false)

  def next():Future[Option[String]] = navigation.map(nav =>
    (hasNext(), Navigation.indexInPage(nav)) match {
      case (false, _) => Future.successful(None)
      case (true, nav.pageLength) => nextPage()
      case (true, i) => Future.successful(nav.pageIDs.lift(i+1 -1))
    }).getOrElse(Future.successful(None))

  def previous():Future[Option[String]] = navigation.map(nav =>
    (hasPrevious(), Navigation.indexInPage(nav)) match {
      case (false, _) => Future.successful(None)
      case (true, 1) => prevPage()
      case (true, i) => Future.successful(nav.pageIDs.lift(i-1 -1))
    }).getOrElse(Future.successful(None))

  def first():Future[Option[String]] = navigation.map(nav =>
    (hasPrevious(), hasPreviousPage()) match {
      case (false, _) => Future.successful(None)
      case (true, true) => firstPage()
      case (true, false) => Future.successful(Session.getIDs().get.ids.lift(1 -1))
    }).getOrElse(Future.successful(None))

  def last():Future[Option[String]] = navigation.map(nav =>
    (hasNext(), hasNextPage()) match {
      case (false, _) => Future.successful(None)
      case (true, true) => lastPage()
      case (true, false) => Future.successful(nav.pageIDs.lift(Navigation.maxIndexLastPage(nav) -1))
    }).getOrElse(Future.successful(None))


  def firstPage():Future[Option[String]] =
    if (!hasPreviousPage()) {
      Future.successful(None)
    }else {
      val newQuery = Session.getQuery().map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = 1)))).getOrElse(JSONQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
    }

  def lastPage():Future[Option[String]] =
    if (!hasNextPage()) {
      Future.successful(None)
    } else {
      val newQuery = Session.getQuery().map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = navigation().map(_.pages).getOrElse(0))))).getOrElse(JSONQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
  }

  def prevPage():Future[Option[String]] =
    if (!hasPreviousPage()) {
      Future.successful(None)
    } else {
      val newQuery = Session.getQuery().map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage = p.currentPage - 1)))).getOrElse(JSONQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.lastOption
      }
    }

  def nextPage():Future[Option[String]] =
    if (!hasNextPage()) {
      Future.successful(None)
    } else {
      val newQuery = Session.getQuery().map(q => q.copy(paging = q.paging.map(p => p.copy(currentPage= p.currentPage + 1)))).getOrElse(JSONQuery.empty)
      REST.ids(kind,Session.lang(),model,newQuery).map{ ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
    }


}
