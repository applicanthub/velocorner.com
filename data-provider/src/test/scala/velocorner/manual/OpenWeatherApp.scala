package velocorner.manual

import org.slf4s.Logging
import velocorner.SecretConfig
import velocorner.feed.{HttpFeed, OpenWeatherFeed}
import velocorner.util.CloseableResource

import scala.concurrent.Await

object OpenWeatherApp extends App with Logging with CloseableResource with MyMacConfig {

  val config = SecretConfig.load()
  withCloseable(new OpenWeatherFeed(config)) { feed =>
    val res = Await.result(feed.query("Zurich,CH"), feed.timeout)
    log.info(s"result is $res")
  }
  HttpFeed.shutdown()
}
