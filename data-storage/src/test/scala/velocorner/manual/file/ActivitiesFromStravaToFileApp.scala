package velocorner.manual.file

import java.io.PrintWriter

import org.slf4s.Logging
import velocorner.SecretConfig
import velocorner.manual.MyMacConfig
import velocorner.proxy.StravaFeed
import velocorner.util.JsonIo


object ActivitiesFromStravaToFileApp extends App with Logging with MyMacConfig {

  implicit val feed = new StravaFeed(None, SecretConfig.load())
  //val activities = feed.recentClubActivities(Club.Velocorner)
  val activities = StravaFeed.listRecentAthleteActivities
  log.info(s"got ${activities.size} athlete activities")

  val json = JsonIo.write(activities)
  val pw = new PrintWriter("/Users/levi/Downloads/strava/all.json")
  pw.print(json)
  pw.close()
  log.info("file has been created")
}
