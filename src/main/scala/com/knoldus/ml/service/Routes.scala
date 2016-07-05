package com.knoldus.ml.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import com.knoldus.ml.repo.Ratings
import org.apache.spark.mllib.recommendation.Rating
import spray.json.DefaultJsonProtocol


trait Routes extends SprayJsonSupport with DefaultJsonProtocol with CORSSupport {
  rating: Ratings =>

  implicit val studentFormat = jsonFormat3(Rating)

  val route = corsHandler {

    path("movies" / "all") {
      get {
        complete {
          getTopTenMovie
        }
      }
    } ~
      path("movies" / "save") {
        post {
          entity(as[Array[(Int, Double)]]) { rating =>
            complete {
              sendResult(rating)
            }
          }
        }
      }
  }

}