package ch.wsl.box.client.utils

import ch.wsl.box.client.services.REST
import ch.wsl.box.model.shared.{IDs, JSONQuery}

import scala.concurrent.Future
import ch.wsl.box.client.Context._
import ch.wsl.box.client.styles.GlobalStyles
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssStyleName
import io.udash.properties.HasModelPropertyCreator
import org.scalajs.dom.Event
import scalatags.JsDom.all.{disabled, onclick}

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
  def indexInPage(nav:Navigation) = {                //1-based
      val i = nav.currentIndex % nav.pageLength
      if (i==0) nav.pageLength else i
    }
  def maxIndexLastPage(nav:Navigation) = (nav.count -1) % nav.pageLength + 1  //1-based

  import io.udash._
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  def button(navProp:ReadableProperty[Boolean],callback: () => Unit,label:String,pull:BootstrapStyles.type => CssStyleName) = scalatags.JsDom.all.button(
    disabled.attrIfNot(navProp),
    pull(BootstrapStyles),ClientConf.style.boxButton,
    onclick :+= ((ev: Event) => callback(), true),
    label
  )

  def pageCount(recordCount:Int) = math.ceil(recordCount.toDouble / ClientConf.pageLength.toDouble).toInt
}



case class Navigator(currentId:Option[String], kind:String = "", model:String = "") {

  def navigation():Option[Navigation] = for {
    id <- currentId
    ids <- Session.getIDs()
    session <- Session.getQuery()
    (_, indexInPage_0based) <- ids.ids.zipWithIndex.find(_._1 == id)
  } yield {

    Navigation(
      hasNext = !(indexInPage_0based == ids.ids.size-1 && ids.isLastPage),
      hasPrevious =  !(indexInPage_0based == 0 && session.query.currentPage == 1),
      count = ids.count,
      currentIndex = (session.query.currentPage-1)*session.query.pageLength(ids.ids.size) + indexInPage_0based + 1,
      hasNextPage = !ids.isLastPage,
      hasPreviousPage = ids.currentPage>1,
      pages = Navigation.pageCount(ids.count),
      currentPage = session.query.currentPage,
      pageLength = session.query.paging.map(_.pageLength).getOrElse(ids.ids.size),
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
      case (true, true) => {
        val newQuery:SessionQuery = Session.getQuery().map(q => q.copy(query = q.query.copy(paging = q.query.paging.map(p => p.copy(currentPage = navigation().map(_.pages).getOrElse(0)))))).getOrElse(SessionQuery.empty)
        REST.ids(kind, Session.lang(), model, newQuery.query).map { ids =>
          Session.setQuery(newQuery)
          Session.setIDs(ids)
          ids.ids.lastOption
        }
      }
      case (true, false) => Future.successful(nav.pageIDs.lift(Navigation.maxIndexLastPage(nav) -1))
    }).getOrElse(Future.successful(None))


  def firstPage():Future[Option[String]] =
    if (!hasPreviousPage()) {
      Future.successful(None)
    }else {
      val newQuery:SessionQuery = Session.getQuery().map(q => q.copy(query = q.query.copy(paging = q.query.paging.map(p => p.copy(currentPage = 1))))).getOrElse(SessionQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery.query).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
    }

  def lastPage():Future[Option[String]] =
    if (!hasNextPage()) {
      Future.successful(None)
    } else {
      val newQuery:SessionQuery = Session.getQuery().map(q => q.copy(query = q.query.copy(paging = q.query.paging.map(p => p.copy(currentPage = navigation().map(_.pages).getOrElse(0)))))).getOrElse(SessionQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery.query).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
  }

  def prevPage():Future[Option[String]] =
    if (!hasPreviousPage()) {
      Future.successful(None)
    } else {
      val newQuery:SessionQuery = Session.getQuery().map(q => q.copy(query = q.query.copy(paging = q.query.paging.map(p => p.copy(currentPage = p.currentPage - 1))))).getOrElse(SessionQuery.empty)
      REST.ids(kind, Session.lang(), model, newQuery.query).map { ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.lastOption
      }
    }

  def nextPage():Future[Option[String]] =
    if (!hasNextPage()) {
      Future.successful(None)
    } else {
      val newQuery:SessionQuery = Session.getQuery().map(q => q.copy(query = q.query.copy(paging = q.query.paging.map(p => p.copy(currentPage= p.currentPage + 1))))).getOrElse(SessionQuery.empty)
      REST.ids(kind,Session.lang(),model,newQuery.query).map{ ids =>
        Session.setQuery(newQuery)
        Session.setIDs(ids)
        ids.ids.headOption
      }
    }


}
