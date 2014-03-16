package de.blackpinguin.mediaindexer

import java.io.{File, FileReader, BufferedReader}

import de.blackpinguin.util.Properties

object ConfigEntry{
  
  Properties.addDefault(Map(
        ("unmatched.show", "true")
      , ("unmatched.conf", """"\A(.*)\z", "\semester", false, "Unsortiert", false, "\1", true""")
  ))
  
  val file = new File("match.conf")
  
  val showUnmatched = Properties("unmatched.show").toBoolean
  lazy val unmatched = parse(Properties("unmatched.conf"))
  
  def parse(entry: String): ConfigEntry  = {
    val split = entry.split(",")
    val matchy = split(0).trim
      
    def toupled(i: Int = 1, accu: List[(String,Boolean)] = List[(String,Boolean)]()): List[(String,Boolean)] = {
      if(i >= split.length) accu
      else {
        val a = split(i).trim
        val b = split(i+1).trim
        toupled(i+2, accu :+ (a.substring(1, a.length-1), b.toBoolean))
      }
    }  
    ConfigEntry(matchy.substring(1, matchy.length - 1), toupled())
  }
  
  lazy val entries: List[ConfigEntry] = {
    //lese die ganze Datei als String ein
    val br = new BufferedReader(new FileReader(file)) 
    var str = ""
    var reading = true
    while(reading){
      val n = br.readLine()
      if(n == null) reading = false
      else str += n
    }
    
    //parse die Config Datei
    for(entry <- str.split(";").toList)
      yield parse(entry)
  }
  
  //alle Layer eines Videos ermitteln
  def layers(video: Video): List[Layer] = {
    val layers = for{
      entry <- entries
      layer = entry.layer(video)
      if(layer != null)
    } yield layer
    
    if(layers.size==0 && showUnmatched)
      List(unmatched.layer(video))
    else
      layers
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