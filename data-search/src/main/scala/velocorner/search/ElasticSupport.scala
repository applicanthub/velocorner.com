package velocorner.search

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.indexes.{IndexApi, IndexDefinition}
import velocorner.model.Activity
import com.sksamuel.elastic4s.http.ElasticDsl._

/**
  * Created by levi on 21.03.17.
  */
trait ElasticSupport extends IndexApi {

  def elasticCluster() = HttpClient(ElasticsearchClientUri("localhost", 9200))

  def map2Indices(activities: Iterable[Activity]): Iterable[IndexDefinition] = {
    activities.map { a =>
      val ixDefinition = indexInto("velocorner" / "activity")
      extractIndices(a, ixDefinition).withId(a.id.toString)
    }
  }

  def extractIndices(a: Activity, id: IndexDefinition): IndexDefinition = id.fields(
    "name" -> a.name,
    "start_date" -> a.start_date,
    "distance" -> a.distance / 1000,
    "elevation" -> a.total_elevation_gain,
    "average_speed" -> a.average_speed.getOrElse(0f),
    "max_speed" -> a.max_speed.getOrElse(0f),
    "average_temp" -> a.average_temp.getOrElse(0f),
    "average_watts" -> a.average_watts.getOrElse(0f)
  )
}