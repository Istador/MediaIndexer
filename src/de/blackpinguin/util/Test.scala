package de.blackpinguin.util

import scala.collection.mutable.{ArrayBuffer => A}

object Test extends App {
  val rnd = new java.security.SecureRandom()
  var a = A[Int]()
  val b = A[Int]()
  val n = 20000
  
  
  println("1:")
  Time.measureAndPrint{
    for(i <- 0 to n){
      a += rnd.nextInt
      a = a.sorted
    }
  }
  
  println("2:")
  Time.measureAndPrint{
    for(i <- 0 to n){
      b.insertSorted(rnd.nextInt)
    }
  }
}