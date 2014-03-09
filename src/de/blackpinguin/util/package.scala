package de.blackpinguin

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

package object util {
  
  def waitFor[A](f:Future[A])(implicit atMost: Duration = Duration.Inf): Unit = {
    f.onFailure { case x:Throwable => x.printStackTrace }
    Await.ready(f, atMost)
  }
  
  def result[A](f: Future[A])(implicit atMost: Duration = Duration.Inf): A = {
    f.onFailure { case x:Throwable => x.printStackTrace }
    Await.result(f, atMost)
  }
  
  
}