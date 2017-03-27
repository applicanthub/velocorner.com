package velocorner.manual.spark

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import velocorner.spark.LocalSpark


object TopNWordsApp extends App with LocalSpark[Seq[(String, Int)]] {

  val n = 20

  override def sparkAppName: String = s"Top $n Words"

  override def spark(sc: SparkContext): Seq[(String, Int)] = {
    val rdd: RDD[String] = sc.textFile("data-cruncher/src/test/resources/data/book/kipling.txt")
    val input = rdd.map(line => line.toLowerCase)
    val result = input
      .flatMap(line => line.split( """\W+"""))
      .map(word => (word, 1))
      .reduceByKey(_ + _)
      .takeOrdered(n)(Ordering[Int].reverse.on(_._2))
      .toSeq

    log.info(s"result: $result")
    result
  }

  proceed()
}