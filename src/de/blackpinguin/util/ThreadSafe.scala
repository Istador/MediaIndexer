package de.blackpinguin.util

object ThreadSafe {
  def apply[T](init: => T): ThreadSafe[T] = {
    new ThreadSafe[T](init)
  }
}

class ThreadSafe[T](init: => T){
  private[this] val tl = new ThreadLocal[T](){
    override def initialValue():T = init
  }
  
  def get: T = tl.get
  def set(x: T): Unit = tl.set(x)
  def remove: Unit = tl.remove
}