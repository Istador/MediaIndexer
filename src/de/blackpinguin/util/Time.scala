package de.blackpinguin.util

object Time {
  
  
  
  def measure[A](f: => A): (A, Long) = {
    val start = System.nanoTime
    val result = f
    val time = System.nanoTime - start
    (result, time)
  }
  
  
  
  private[this] def next(old: Output, amount: Long, unit: String): Output = {
    if (old.time1 >= amount)
      Output(old.time1 / amount, unit, old.time1 % amount, old.unit1)
    else
      old
  }
  
  
  
  private[this] case class Output(time1: Long, unit1: String, time2: Long, unit2: String) {
    override def toString: String = time1 + " " + unit1 + " " + time2 + " " + unit2
  }
  
  
  
  def measureAndPrint[A](f: => A): A = {
    var (result, time) = measure(f)

    var out = Output(time / 1000, "µs", time % 1000, "ns")
    out = next(out, 1000, "ms")

    if (out.time1 >= 1000) {
      out = next(out, 1000, "s")
      out = next(out, 60, "m")
      out = next(out, 60, "h")
    }

    println("Time: " + out)
    result
  }
  
  
  
}