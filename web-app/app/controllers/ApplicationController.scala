package controllers

import javax.inject.Inject

import controllers.auth.AuthChecker
import play.Logger
import play.api.cache.SyncCacheApi
import play.api.mvc._

import scala.concurrent.Future

class ApplicationController @Inject()
(components: ControllerComponents, val cache: SyncCacheApi,
 val connectivity: ConnectivitySettings, strategy: RefreshStrategy)
(implicit assets: AssetsFinder) extends AbstractController(components) with AuthChecker {

  def index = AuthAction { implicit request =>
    Ok(views.html.index(getPageContext("Home")))
  }

  def refresh = AuthAsyncAction { implicit request =>
    val maybeAccount = loggedIn
    Logger.info(s"refreshing page for $maybeAccount")
    maybeAccount.foreach(strategy.refreshAccountActivities)
    Future.successful(Redirect(routes.ApplicationController.index()))
  }

  def search = AuthAction { implicit request =>
    Ok(views.html.search(getPageContext("Search")))
  }

  def about = AuthAction { implicit request =>
    Ok(views.html.about(getPageContext("About")))
  }

  private def getPageContext(title: String)(implicit request: Request[AnyContent]) = {
    val maybeAccount = loggedIn
    val context = PageContext(title, maybeAccount,
      connectivity.secretConfig.isWithingsEnabled(),
      connectivity.secretConfig.isWeatherEnabled(), WeatherCookie.retrieve
    )
    Logger.info(s"rendering ${title.toLowerCase} page for $maybeAccount")
    context
  }
}