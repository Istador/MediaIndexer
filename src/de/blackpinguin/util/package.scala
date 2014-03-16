package de.blackpinguin

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

package object util {
  
  def waitFor[A](f:Future[A])(implicit atMost: Duration = Duration.Inf): Unit = {
    Await.ready(f.andThen {
      case Success(v) => 
      case Failure(e) => LogError(e) := "Exception in Future "+f
    }, atMost)
  }
  
  def result[A](f: Future[A])(implicit atMost: Duration = Duration.Inf): A = {
    Await.result(f.andThen {
      case Success(v) => v 
      case Failure(e) => 
        LogError(e) := "Exception in Future "+f
        null
    }, atMost)
  }
  
  
}