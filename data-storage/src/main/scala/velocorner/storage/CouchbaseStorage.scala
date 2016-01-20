package velocorner.storage

import java.net.URI
import java.util.concurrent.TimeUnit

import com.couchbase.client.CouchbaseClient
import com.couchbase.client.protocol.views._
import org.slf4s.Logging
import velocorner.model.{Account, DailyProgress, Progress, Activity}
import velocorner.util.JsonIo

import scala.collection.JavaConversions._


class CouchbaseStorage(password: String) extends Storage with Logging {

  lazy val uri = URI.create("http://localhost:8091/pools")
  lazy val client = new CouchbaseClient(List(uri), "velocorner", password)

  val progressDesignName = "progress"
  val progressByDayViewName = "by_day"

  val listDesignName = "list"
  val allActivitiesViewName = "all_activities"
  val activitiesByDateViewName = "activities_by_date"
  val allAccountsViewName = "all_accounts"


  // activities
  override def store(activities: Iterable[Activity]) {
    // TODO: bulk store
    activities.foreach{a =>
      client.set(a.id.toString, 0, JsonIo.write(a))
    }
  }

  override def dailyProgress(athleteId: Int): Iterable[DailyProgress] = {
    val view = client.getView(progressDesignName, progressByDayViewName)
    val query = new Query()
    query.setGroup(true)
    query.setStale(Stale.FALSE)
    query.setInclusiveEnd(true)
    query.setRange(s"[$athleteId, [2000, 1, 1]]", s"[$athleteId, [3000, 12, 31]]")
    val response = client.query(view, query)
    for (entry <- response) yield DailyProgress.fromStorage(entry.getKey, entry.getValue)
  }

  override def listAllActivityIds(): Iterable[Int] = queryForIds(client.getView(listDesignName, allActivitiesViewName)).map(_.toInt)

  override def listRecentActivities(athleteId: Option[Int], limit: Int): Iterable[Activity] = {
    val view = client.getView(listDesignName, activitiesByDateViewName)
    val query = new Query()
    query.setStale(Stale.FALSE)
    query.setInclusiveEnd(true)
    query.setDescending(true)
    query.setLimit(limit)
    val (dateFrom, dateTo) = ("[3000, 1, 1]", "[2000, 12, 31]")
    val (rangeFrom, rangeTo) = athleteId.map(aid => (s"[$dateFrom, $aid]", s"[$dateTo, $aid]")).getOrElse((s"[$dateFrom, 1]", s"[$dateTo, ${Int.MaxValue}]"))
    query.setRange(rangeFrom, rangeTo)
    query.setIncludeDocs(true)
    val response = client.query(view, query)
    for (entry <- response) yield JsonIo.read[Activity](entry.getDocument.toString)
  }

  override def deleteActivities(ids: Iterable[Int]) {
    ids.map(_.toString).foreach(client.delete)
  }


  // accounts
  override def store(account: Account) {
    client.set(s"account_${account.athleteId.toString}", 0, JsonIo.write(account))
  }

  override def getAccount(id: Long): Option[Account] = {
    Option(client.get(s"account_$id")).map(json => JsonIo.read[Account](json.toString))
  }

  override def listAllAccountIds(): Iterable[String] = queryForIds(client.getView(listDesignName, allAccountsViewName))

  private def queryForIds(view: View): Iterable[String] = {
    val query = new Query()
    query.setStale(Stale.FALSE)
    val response = client.query(view, query)
    for (entry <- response) yield entry.getId
  }

  // initializes any connections, pools, resources needed to open a storage session, creates the design documents
  override def initialize() {
    client.deleteDesignDoc(progressDesignName)
    val progressDesign = new DesignDocument(progressDesignName)
    val mapProgress =
      """
        |function (doc, meta) {
        |  if (doc.type && doc.type == "Ride" && doc.start_date && doc.athlete && doc.distance) {
        |    var d = dateToArray(doc.start_date)
        |    emit([doc.athlete.id, [d[0], d[1], d[2]]],
        |         {
        |           distance: doc.distance,
        |           elevation: doc.total_elevation_gain,
        |           time: doc.moving_time
        |         });
        |  }
        |}
      """.stripMargin
    val reduceProgress =
      """
        |function(key, values, rereduce) {
        |  var res = {ride: 0,
        |             dist: 0,
        |             distmax: 0,
        |             elev: 0,
        |             elevmax: 0,
        |             time: 0};
        |  for(i=0; i < values.length; i++) {
        |    if (rereduce) {
        |      res.ride += values[i].ride;
        |      res.dist += values[i].dist;
        |      res.distmax = Math.max(res.distmax, values[i].dist);
        |      res.elev += values[i].elev;
        |      res.elevmax = Math.max(res.elevmax, values[i].elev);
        |      res.time += values[i].time;
        |    } else {
        |      res.ride += 1;
        |      res.dist += values[i].distance;
        |      res.distmax = Math.max(res.distmax, values[i].distance);
        |      res.elev += values[i].elevation;
        |      res.elevmax = Math.max(res.elevmax, values[i].elevation);
        |      res.time += values[i].time;
        |    }
        |  }
        |  return res;
        |}
      """.stripMargin
    val byDayView = new ViewDesign(progressByDayViewName, mapProgress, reduceProgress)
    progressDesign.getViews.add(byDayView)
    client.createDesignDoc(progressDesign)

    client.deleteDesignDoc(listDesignName)
    val listDesign = new DesignDocument(listDesignName)
    listDesign.getViews.add(new ViewDesign(allActivitiesViewName, mapForAllIdsFor("Ride")))
    listDesign.getViews.add(new ViewDesign(allAccountsViewName, mapForAllIdsFor("Account")))
    val mapAccountsByDate =
      """
        |function (doc, meta) {
        |  if (doc.type && doc.type == "Ride") {
        |    var d = dateToArray(doc.start_date)
        |    emit([[d[0], d[1], d[2]], doc.athlete.id], doc.id);
        |  }
        |}
      """.stripMargin
    listDesign.getViews.add(new ViewDesign(activitiesByDateViewName, mapAccountsByDate))
    client.createDesignDoc(listDesign)
  }

  // releases any connections, resources used
  override def destroy() {
    client.shutdown(1, TimeUnit.SECONDS)
  }

  private def mapForAllIdsFor(docType: String) = {
    s"""
      |function (doc, meta) {
      |  if (doc.type && doc.type == "$docType") {
      |    emit(meta.id, null);
      |  }
      |}
    """.stripMargin
  }
}
