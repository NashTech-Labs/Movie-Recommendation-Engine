package com.knoldus.ml.repo

/**
  * Created by akash on 4/7/16.
  */
trait Ratings {

  def getTopTenMovie: Array[(Int, String)]

  def sendResult(data: Array[(Int, Double)]): Array[(Int, String)]

}
