package velocorner.search

import java.nio.file.Files

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.slf4s.Logging
import org.specs2.mutable.Specification
import velocorner.model.strava.Activity
import velocorner.util.JsonIo


class ElasticSupportSpec extends Specification with ElasticSupport with Logging {

  sequential

  "local node" should {

    val path = Files.createTempDirectory("elastic")
    log.info(s"temporary workspace is $path")

    val localSettings = Settings.builder()
      .put("http.enabled", true)
      .put("processors", 1)
      .put("path.home", path.toString)
      .put("path.data", path.resolve("data").toString)
      .put("path.repo", path.resolve("repo").toString)
      .put("cluster.name", "test-velocorner")
    val localNode = LocalNode(localSettings.build())
    val client = localNode.client(shutdownNodeOnClose =  true)

    "create indices" in {
      val activities = JsonIo.readReadFromResource[List[Activity]]("/data/strava/last30activities.json")
      activities must haveSize(30)
      val indices = map2Indices(activities).map(_.refresh(RefreshPolicy.IMMEDIATE))
      val res = indices.map(ix => client.execute(ix).await)
      val statuses = res.map(_.status)
      statuses must haveSize(30) // it has 6 skiing events
      statuses must contain(201)
    }

    "search" in {
      val res = client.execute(searchWithType("velocorner" / "activity") matchQuery("name", "Uetli") limit 5).await
      log.info(s"found $res")

      res.status === 200
      val hits = res.result.hits.hits

      log.info(s"search results ${hits.mkString(",")}")
      hits.length === 5
    }

    step {
      client.close()
      localNode.stop(true)
      val file = path.toFile
      if (file.exists()) file.delete()
    }
  }
}
