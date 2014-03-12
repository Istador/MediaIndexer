package de.blackpinguin.mediaindexer

import scala.concurrent.duration.Duration
import de.blackpinguin.util.DOM._


case class VFile(url: String, _typ:String){
  lazy val typ: String = _typ.split("/")(1)
}



object Video {
  
  import XML.doc //um es implicit zu verwenden
  
  //
  def fromXML(node: N):Video = {
    val v = Video( node.attr("url").attr )

    v.title = node.attr("title").attr
    v.author = node.attr("author").attr
    v.duration = node.attr("duration").attr
    v.pubDate = node.attr("pubdate").attr
    
    for(file <- node.getChildNodes:NL){
      val url = file.attr("url").attr
      val typ = file.attr("type").attr
      v.add(VFile(url, "video/"+typ))      
    }
    
    v
  }
  
  
  def latest: Int = latestId
  
  private[this] var latestId:Int = xpath("/indexer[1]/videos[1]/@latest").attr.toInt
  
  private[Video] def getID(url:String):Int = {
    val node = xpath("/indexer[1]/videos[1]/video[@url='"+url+"']")
    if(node.size == 1)
      node.attr("id").attr.toInt
    else{
      this.synchronized{
    	latestId += 1
    	latestId
      }
    }
  }
   
  implicit object vlordering extends Ordering[Video] {
    def compare(a: Video, b: Video) =
      a.title compare b.title
  }
}



case class Video(val url: String) {
  
  import de.blackpinguin.util.Dates._
  
  //eindeutige ID
  val id:Int = Video.getID(url)
  
  //original Titel aus der Mediathek (kein substring)
  var title: String = null
  
  def title(conf: ConfigEntry): String = conf.title(this)
  
  //Benutzerkennung des Accounts der das Video hochgeladen hat
  var author: String = null
  
  //Dauer
  var duration: Duration = null
  
  //Veroeffentlichungsdatum
  var pubDate: Date = null
  
  //Datum aus dem Title, oder falls nicht vorhanden aus pubDate
  lazy val date: Date = {
    val d = Date(title)
    if(d == null) pubDate
    else d
  }
  
  //Semester
  lazy val semester: String = {
    if (date.month < 3) (date.year - 1)+" WiSe"
    else if(date.month < 9) date.year+" SoSe"
    else date.year+" WiSe"
  }
 
  
  //Dateien
  lazy val files = scala.collection.mutable.MutableList[VFile]()
  def add(file: VFile) = {
    files += file
  }
  
  
}

