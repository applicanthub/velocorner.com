package controllers

import jp.t2v.lab.play2.auth.AuthConfig
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import velocorner.model.{Account, Permission}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._

/**
  * Created by levi on 30/11/15.
  */
trait AuthConfigSupport extends AuthConfig {

  type Id = Long

  type User = Account

  type Authority = Permission

  val idTag: ClassTag[Id] = classTag[Id]
  val sessionTimeoutInSeconds: Int = 3600

  override def resolveUser(id: Long)(implicit context: ExecutionContext): Future[Option[Account]] = {
    Logger.info(s"resolving user[$id]")
    Future.successful(Global.getStorage.getAccount(id))
  }

  override def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index()))
  }

  override def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index()))
  }

  override def authorize(user: Account, authority: Permission)(implicit context: ExecutionContext): Future[Boolean] = {
    Future.successful(true)
  }

  override def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Redirect(routes.Application.index()))
  }


  override def authorizationFailed(request: RequestHeader, user: Account, authority: Option[Permission])(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Forbidden("no permission"))
  }
}