package de.blackpinguin

import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.collection.mutable.ArrayBuffer

package object util {
  
  lazy val CheckedException = BehandelteException()
  case class BehandelteException() extends RuntimeException
    
  private[this] def analyseException[A](f: Future[A], e: Throwable): Unit = {
    e match {
      case be: BehandelteException =>
        
      case ee: java.util.concurrent.ExecutionException =>
        ee.getCause match {
          case be: BehandelteException =>
            
          case c: HTTP.StatusException =>
          	LogError(e) := "Fehler beim Laden von '"+c.url+"'. HTTP Status Code: "+c.status
          case c: java.net.ConnectException =>
          	LogError(e) := "Fehler beim Verbindungsaufbau"
          case c: java.io.IOError =>
          	LogError(e) := "Fehler mit der Verbindung"
          case c: Throwable => 
          	LogError(e) := "Unknown Exception in Future "+f
        }
      case t: Throwable =>
        LogError(e) := "Unknown Exception in Future "+f
    }
  }
  
  def waitFor[A](f:Future[A])(implicit atMost: Duration = Duration.Inf): Unit = {
    Await.ready(f.andThen {
      case Success(v) => 
      case Failure(e) => analyseException(f, e)
    }, atMost)
  }
  
  def result[A](f: Future[A])(implicit atMost: Duration = Duration.Inf): A = {
    var ex = false
    val r = Await.result(f.andThen {
      case Success(v) => v
      case Failure(e) => 
        analyseException(f, e)
        ex = true
    }, atMost)
    
    if(ex)
      throw CheckedException
    else r
  }
  
  implicit class ArrayInsertSort[T](arr: ArrayBuffer[T]) {
	  def insertSorted(elem: T)(implicit o: Ordering[T]): Unit = {
	    arr.append(elem)
	    
	    def swap(i: Int, j: Int) = {
	      val tmp = arr(i)
	      arr.update(i, arr(j))
	      arr.update(j, tmp)
	    }
	    
	    var i = arr.length - 1
	    while(i > 0 && o.lt(arr(i), arr(i-1)) ){
	      swap(i, i-1)
	      i = i-1
	    }
	  }
  }
  
}