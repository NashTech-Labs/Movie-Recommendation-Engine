package com.knoldus.ml.repo

import java.util.Scanner

import com.knoldus.ml.service.MovieRequestController
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import com.typesafe.config.ConfigFactory


trait RecommendMovie {

  val ratingPath = ConfigFactory.load().getString("ratings.directory")
  val moviePath = ConfigFactory.load().getString("movies.directory")

  val conf = new SparkConf().setAppName("Recommendation App").setMaster("local")
  val sc = new SparkContext(conf)
  val scanner = new Scanner(System.in)
  val numPartitions = 20


  def getRatingRDD(ratingFilePath: String): RDD[String] = sc.textFile(ratingFilePath)

  def getMovieRDD(movieFilePath: String): RDD[String] = sc.textFile(movieFilePath)


  def getRDDOfRating: RDD[(Long, Rating)] = {

    getRatingRDD(ratingPath).map { line => val fields = line.split("::")

      (fields(3).toLong % 10, Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble))
    }
  }

  def getMoviesMap: Map[Int, String] = {

    getMovieRDD(moviePath).map { line => val fields = line.split("::")
      (fields(0).toInt, fields(1))
    }.collect().toMap
  }

  def getTopTenMovies: List[(Int, String)] = {

    val top50MovieIDs = getRDDOfRating.map { rating => rating._2.product }
      .countByValue()
      .toList
      .sortBy(-_._2)
      .take(50)
      .map { ratingData => ratingData._1 }

    top50MovieIDs.filter(id => getMoviesMap.contains(id))
      .map { movieId => (movieId, getMoviesMap.getOrElse(movieId, "No Movie Found")) }
      .sorted
      .take(10)
  }


  def getResult(datas: Array[(Int, Double)]): Array[(Int, String)] = {

    def getRatingFromUser(data: Array[(Int, Double)]): RDD[Rating] = {

      val listOFRating = data.map { getRating => {
        Rating(0, getRating._1, getRating._2)
      }
      }
      sc.parallelize(listOFRating)
    }

    val myRatingsRDD = getRatingFromUser(datas)
    val data = sc.textFile(ratingPath)
    val ratings = data.map(_.split("::") match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    })
    val movies = getMovieRDD(moviePath).map(_.split("::"))
      .map { case Array(movieId, movieName, genre) => (movieId.toInt, movieName) }

    val training = ratings.filter { case Rating(userId, movieId, rating) => (userId * movieId) % 10 <= 5 }.persist
    val test = ratings.filter { case Rating(userId, movieId, rating) => (userId * movieId) % 10 > 5 }.persist

    val model = ALS.train(training.union(myRatingsRDD), 8, 10, 0.01)
    val moviesIHaveSeen = myRatingsRDD.map(x => x.product).collect().toList
    val moviesIHaveNotSeen = movies.filter { case (movieId, name) => !moviesIHaveSeen.contains(movieId) }.map(_._1)

    val predictedRates =
      model.predict(test.map { case Rating(user, item, rating) => (user, item) }).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      }.persist()

    val ratesAndPreds = test.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictedRates)

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) => Math.pow((r1 - r2), 2) }.mean()

    println("Mean Squared Error = " + MSE)

    val recommendedMoviesId = model.predict(moviesIHaveNotSeen.map { product =>
      (0, product)
    }).map { case Rating(user, movie, rating) => (movie, rating) }
      .sortBy(x => x._2, ascending = false).take(20).map(x => x._1)

    getMovieRDD(moviePath).map { movie =>
      val field = movie.split("::")
    }
    val idList = recommendedMoviesId.toList
    val sendResult = getMovieRDD(moviePath).filter { line => idList.contains(line.split("::")(0).toInt) }
      .map { movie =>
        val field = movie.split("::")
        (field(0).toInt, field(1) + " - " + field(2))
      }.collect()
    sendResult
  }
}