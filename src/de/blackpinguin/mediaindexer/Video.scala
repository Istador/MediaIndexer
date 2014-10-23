package de.blackpinguin.mediaindexer

import scala.concurrent.duration.Duration
import de.blackpinguin.util.DOM._
import org.w3c.dom.Document


case class VFile(url: String, _typ:String){
  lazy val typ: String = _typ.split("/")(1)
  
  def toXML(implicit doc: Document): N = {
    val node = doc.createElement("file")
    node.attr("url", url)
    node.attr("type", typ)
    node
  }
}



object Video {
  
  import XML.doc //um es implicit zu verwenden
  
  //
  def fromXML(node: N):Video = {
    val v = Video( node.attr("url").attr )
    
    v.title = node.attr("title").attr
    v.author = node.attr("author").attr
    v.pubDate = node.attr("pubdate").attr
    
    val duration = node.attr("duration")
    if(duration != null) v.duration = duration.attr
    
    val comments = node.attr("comments")
    if(comments != null) v.comments = comments.attr.toInt
    
    for(file <- node.getChildNodes:NL){
      val url = file.attr("url").attr
      val typ = file.attr("type").attr
      v.add(VFile(url, "video/"+typ))      
    }
    
    v
  }
  
  
  def latest: Int = latestId
  
  private[this] var latestId:Int = xpath("/indexer[1]/videos[1]/@latest").attr.toInt
  
  private[Video] def getID(url:String):(Int, Int) = {
    val node = xpath("/indexer[1]/videos[1]/video[@url='"+url+"']")
    if(node.size == 1){
      val comments = node.attr("comments")
      (node.attr("id").attr.toInt, if(comments==null) 0 else comments.attr.toInt) 
    } else {
      this.synchronized{
    	latestId += 1
    	(latestId, 0)
      }
    }
  }
   
  implicit object vlordering extends Ordering[Video] {
    def compare(a: Video, b: Video) =
      a.title compare b.title
  }
}



case class Video(val url: String) extends Iterable[VFile] {
  
  import de.blackpinguin.util.Dates._
  
  //Anzahl Kommentare
  var comments: Int = 0
  
  //eindeutige ID
  val id = {
    val tmp = Video.getID(url)
    comments = tmp._2
    tmp._1
  }
  
  var commentsChanged: Int = 0
  
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
  
  def iterator: Iterator[VFile] = files.iterator
  
  def toXML(implicit doc: Document): N = {
    val node: N = doc.createElement("video")
    node.attr("id", id)
    node.attr("url", url)
    node.attr("title", title)
    node.attr("date", date)
    node.attr("pubdate", pubDate)
    node.attr("author", author)
    
    //Kommentaranzahl, wenn nicht 0
    if(comments != 0)
      node.attr("comments", comments)
    
    //Dauer falls bekannt
    if (duration != null)
      node.attr("duration", duration)
    
    //füge <file>-Elemente hinzu
    this.foreach {
      node appendChild _.toXML
    }
    
    node
  }
  
}

