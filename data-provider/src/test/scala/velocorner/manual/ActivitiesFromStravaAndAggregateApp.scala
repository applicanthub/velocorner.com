package velocorner.manual

import org.slf4s.Logging
import velocorner.SecretConfig
import velocorner.feed.{HttpFeed, StravaActivityFeed}
import velocorner.storage.Storage
import velocorner.util.Metrics

object ActivitiesFromStravaAndAggregateApp extends App with Logging with Metrics with AggregateActivities with MyMacConfig {

  private val config = SecretConfig.load()
  implicit val feed = new StravaActivityFeed(None, config)

  val storage = Storage.create("or")
  storage.initialize()
  val activities = StravaActivityFeed.listRecentAthleteActivities
  log.info(s"retrieved ${activities.size} activities")
  storage.storeActivity(activities)

  val progress = timed("aggregation")(storage.dailyProgressForAthlete(432909))
  printAllProgress(progress)

  log.info("done...")
  storage.destroy()
  feed.close()
  HttpFeed.shutdown()
}
