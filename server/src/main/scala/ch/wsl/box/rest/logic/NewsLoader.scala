package ch.wsl.box.rest.logic

import ch.wsl.box.model.shared.NewsEntry
import ch.wsl.box.model.boxentities.BoxNews
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.shared.utils.DateTimeFormatters
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object NewsLoader {

  def get(lang:String)(implicit userProfile: UserProfile, ec:ExecutionContext): DBIO[Seq[NewsEntry]] = {
    val q = for{
      news <- BoxNews.BoxNewsTable
      news_i18n <- BoxNews.BoxNews_i18nTable if news_i18n.lang === lang && news.news_uuid === news_i18n.news_uuid
    } yield (news.datetime,news_i18n.title,news_i18n.text,news.author)

    {
      q.sortBy( _._1.desc).result
    }.map{ _.map{ x =>
      NewsEntry(DateTimeFormatters.timestamp.format(x._1),x._2,x._3,x._4)
    }}
  }


}
