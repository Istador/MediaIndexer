package de.blackpinguin

package object mediaindexer {
  
  import scala.concurrent.duration.Duration
  
  import scala.language.implicitConversions
  
  implicit def int2String(x:Int):String = x.toString
  
  implicit def str2Duration(str: String):Duration = {
    var d:Duration = Duration(0, "seconds")
    val comps = str.split(":").map(_.toInt)
    if(comps.length >= 1)
    	d += Duration(comps(comps.length-1), "seconds")
    if(comps.length >= 2)
    	d += Duration(comps(comps.length-2), "minutes")
    if(comps.length >= 3)
    	d += Duration(comps(comps.length-3), "hours")
    d
  }
  
  implicit def Duration2Str(d: Duration):String = {
    val s = d.toSeconds
    val m = s / 60
    val h = m / 60
    var sec = ""+(s%60)
    if(sec.length == 1) sec = "0"+sec
    var min = ""+(m%60)
    if(min.length == 1) min = "0"+min
    if(h == 0)
      min+":"+sec
    else
      h+":"+min+":"+sec
  }
  
}