package ch.wsl.box.client.db

import scala.concurrent.{ExecutionContext, Future}

trait DbEntity[T,K] {
  def init()(implicit ec:ExecutionContext):Future[Boolean]
  def get(k:K)(implicit ec:ExecutionContext):Future[Option[T]]
  def save(o:T)(implicit ec:ExecutionContext):Future[T]
  def delete(o:K)(implicit ec:ExecutionContext):Future[Boolean]
  def list()(implicit ec:ExecutionContext):Future[Seq[T]]
}
