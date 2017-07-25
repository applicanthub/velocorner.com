package velocorner.feed

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.asynchttpclient.{DefaultAsyncHttpClientConfig, Realm}
import org.asynchttpclient.proxy.ProxyServer
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import velocorner.SecretConfig

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created by levi on 25.10.16.
  */
trait HttpFeed {

  val config: SecretConfig

  val httpConfigBuilder = new DefaultAsyncHttpClientConfig.Builder()

  // setup secure proxy if it is configured w/o authentication
  for (proxyHost <- config.getProxyHost; proxyPort <- config.getProxyPort) {
    val proxyServer = (config.getProxyUser, config.getProxyPassword) match {
      case (Some(proxyUser), Some(proxyPassword)) =>
        val realm = new Realm.Builder(proxyUser, proxyPassword).build()
        new ProxyServer(proxyHost, proxyPort, 443, realm, List.empty[String].asJava)
      case _ =>
        new ProxyServer(proxyHost, proxyPort, 443, null, List.empty[String].asJava)
    }
    httpConfigBuilder.setProxyServer(proxyServer)
  }

  implicit val system = ActorSystem.create("ws-feed")
  implicit val materializer = ActorMaterializer()

  private val wsClient = new AhcWSClient(httpConfigBuilder.build())

  def ws[T](func: WSClient => T): T = {
    val client = wsClient
    try {
      func(client)
    } finally {
      //client.close()
    }
  }
}