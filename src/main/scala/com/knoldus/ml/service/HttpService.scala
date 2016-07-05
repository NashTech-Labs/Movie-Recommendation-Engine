package com.knoldus.ml.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.knoldus.ml.repo.{Ratings, RecommendMovie}

//import scala.concurrent.ExecutionContext.Implicits.global


object HttpService extends App with Routes with MovieRequestController {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  Http().bindAndHandle(route, "localhost", 9000)

}

trait MovieRequestController extends Ratings with RecommendMovie {


  def getTopTenMovie: Array[(Int, String)] = {

    getTopTenMovies.toArray
  }

  def sendResult(data: Array[(Int, Double)]): Array[(Int, String)] = {

    getResult(data)
  }

}
