package de.blackpinguin.mediaindexer

import scala.concurrent.duration.Duration



case class VFile(url: String, typ:String)



object Video {
  
  private[this] var latestId:Int = XML.xpath("/indexer[1]/videos[1]/@latest").attr.toInt
  
  private[Video] def getID(url:String):Int = {
    val node = XML.xpath("/indexer[1]/videos[1]/video[@url='"+url+"']")
    if(node.size == 1)
      node.attr("id").attr.toInt
    else{
      latestId += 1
      latestId
    }
  }
  
  //Matcht Datum in einem beliebigen String
  private[Video] val dateRE = """\A.* (\d{4}-\d{2}-\d{2}) .*\z""".r
  
  //Teilt ein Datum in Jahr, Monat und Tag
  private[Video] val dateCompsRE = """\A(\d{4})-(\d{2})-(\d{2})\z""".r
  
   
  implicit object vlordering extends Ordering[Video] {
    def compare(a: Video, b: Video) =
      a.title compare b.title
  }
}



class Video(val url: String) {
  
  //eindeutige ID
  val id:Int = Video.getID(url)
  
  //original Titel aus der Mediathek (kein substring)
  var title: String = null
  
  def title(conf: ConfigEntry): String = conf.title(this)
  
  //Dauer
  var duration: Duration = null
  
  //Veroeffentlichungsdatum
  var pubDate: String = null
  
  //Datum aus dem Title, oder falls nicht vorhanden aus pubDate
  lazy val date: String = {
    Video.dateRE.findFirstMatchIn(title) match {
      case Some(m) => m.group(1)
      case None => pubDate
    }
  }
  
  //Semester
  private[this] lazy val Video.dateCompsRE(y, m, d) = date
  private[this] lazy val (year, month, day) = (y.toInt, m.toInt, d.toInt)
  lazy val semester: String = {
    if (month < 3) (year - 1)+" WiSe"
    else if(month < 9) year+" SoSe"
    else year+" WiSe"
  }
 
  
  //Dateien
  lazy val files = scala.collection.mutable.MutableList[VFile]()
  def add(file: VFile) = {
    files += file
  }
  
  
}

