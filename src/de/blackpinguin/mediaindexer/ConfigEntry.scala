package de.blackpinguin.mediaindexer

import java.io.{File, FileReader, BufferedReader}

object ConfigEntry{
  
  val file = new File("match.conf")
  
  lazy val entries = {
    val r = scala.collection.mutable.MutableList[ConfigEntry]()
    val br = new BufferedReader(new FileReader(file)) 
    var str = ""
    var reading = true
    while(reading){
      val n = br.readLine()
      if(n == null) reading = false
      else str += n
    }
    
    for(entry <- str.split(";")){
      val split = entry.split(",")
      val matchy = split(0).trim
      
      def toupled(i:Int = 1, accu:List[(String,Boolean)] = List[(String,Boolean)]()):List[(String,Boolean)] = {
        if(i >= split.length) accu
        else {
          val a = split(i).trim
          val b = split(i+1).trim
          toupled(i+2, accu :+ (a.substring(1, a.length-1), b.toBoolean))
        }
      }
      
      r += ConfigEntry(matchy.substring(1, matchy.length - 1), toupled())
    }
    
    r.toList
  }
  
  //alle Layer eines Videos ermitteln
  def layers(video: Video): List[Layer] = {
    var layers = List[Layer]()
    for{
      entry <- entries
      layer = entry.layer(video)
      if(layer != null)
    } yield layer
  }
  
}

case class ConfigEntry(val matchStr:String, layers: List[(String, Boolean)]) {
  
  val matcher = matchStr.r
  
  private[this] def layerString(video: Video, str: String):String = {
    if(!str.charAt(0).equals('\\')){
      str
    } else {
      str match {
        case "\\semester" => video.semester 
        case "\\datum" => video.date
        case _ => {
          val g = str.substring(1, str.length).toInt
          matcher.findFirstMatchIn(video.title).get.group(g)
        }
      }
      
    }
  }
  
  def title(video: Video): String = {
    layerString(video, layers.last._1)
  }
  
  def layer(video: Video): Layer = {
    try{
      val lays = layers.map({ t => (layerString(video, t._1), t._2) }) 
      var n = Layer.getLayer(lays(0))
      for(i <- 1 until lays.size-1){
        n = n.getLayer(lays(i))
      }
      n.add(video, title(video))
      n
    } catch {
      //wenn das Video nicht auf den regulären Ausdruck matcht
      case _:NoSuchElementException => null
    }
  } 
    
}