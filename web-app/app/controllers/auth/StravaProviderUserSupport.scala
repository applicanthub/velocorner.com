package controllers.auth

import controllers.ConnectivitySettings
import jp.t2v.lab.play2.auth.social.core.{OAuth2Controller, OAuthProviderUserSupport}
import play.api.Logger
import velocorner.model.Account

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by levi on 09/12/15.
  */
trait StravaProviderUserSupport extends OAuthProviderUserSupport {
  self: OAuth2Controller =>

  override type ProviderUser = Account

  val connectivity: ConnectivitySettings

  override def retrieveProviderUser(accessToken: AccessToken)(implicit ctx: ExecutionContext): Future[ProviderUser] = {
    val token = accessToken.toString
    Logger.info(s"retrieve user for $token")
    val athlete = connectivity.getFeed(token).getAthlete
    Logger.info(s"got athlete for user $athlete")
    Future.successful(Account.from(athlete, token, None))
  }
}
