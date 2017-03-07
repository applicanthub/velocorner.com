package velocorner.manual.spark

import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.slf4s.Logging
import velocorner.SecretConfig
import velocorner.manual.MyMacConfig
import velocorner.model.Activity
import velocorner.spark.CouchbaseConnector
import velocorner.util.Metrics

/**
  * Created by levi on 28/02/16.
  */
object MachineLearningApp extends App with Logging with Metrics with MyMacConfig {

  log.info("connecting to couchbase bucket...")
  val conn = CouchbaseConnector(SecretConfig.load())
  val activities = conn.dailyProgressForAthlete(432909, 10)


  //log.info(s"got ${activities.count()} activities")
  //val data2015 = activities.filter(_.start_date.getYear == 2015)
  //log.info(s"got ${data2015.count()} activities from 2015")

  implicit class FeatureExtractor(activity: Activity) {

    // features:
    // - distance - as label or predicted
    // - month
    // - day
    // - day of week - work days vs weekends
    def features: Array[Double] = Array(
      activity.start_date.getMonthOfYear.toDouble,
      activity.start_date.getDayOfMonth.toDouble,
      activity.start_date.getDayOfWeek.toDouble
    )

    def labeledPoint: LabeledPoint = LabeledPoint(activity.distance.toDouble, Vectors.dense(features))
  }

  // prepare training set
  //val parsedTrainData = data2015.map(_.labeledPoint)
  //val scaler = new StandardScaler(withMean = true, withStd = true).fit(parsedTrainData.map(x => x.features))
  //val scaledTrainData = parsedTrainData.map(x => LabeledPoint(x.label, scaler.transform(Vectors.dense(x.features.toArray))))


  conn.stop
}
