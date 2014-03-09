package de.blackpinguin.mediaindexer

import scala.xml.NodeSeq
import collection.mutable.{Set => Coll}
import collection.immutable.{ SortedSet => Set }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.w3c.dom.Document
import scala.util.Success
import scala.util.Failure
import scala.util.matching.Regex

object Mediathek {
  
  import de.blackpinguin.util._
  import de.blackpinguin.util.AsyncHTTP
  import de.blackpinguin.util.DOM._
  import de.blackpinguin.util.{Properties => Prop}
  
  Prop.addDefault(Map(
        ("domain", "http://mediathek.mt.haw-hamburg.de")
      , ("pages.url", "/media/list/component/boxList/filter/all/limit/all/layout/thumbBig/page/")
      , ("pages.xpath", "//div[@class='pagination']/a/@href")
      , ("videos.xpath", "//div[@id='s-media-box-list']/ul[1]/li")
      , ("video.url.xpath", "./a[@class='play']/@href")
      , ("video.title.xpath", "./a[@class='play']/@title")
      , ("video.duration.xpath", "./div[1]/p[2]")
      , ("video.duration.regex", "Dauer: ((\\d+:)?\\d{2}:\\d{2})")
      , ("video.pubDate.xpath", "//ul[@id='mediaInfo']/li[2]")
      , ("video.author.xpath", "./div[1]/p[1]/a")
      , ("video.files.xpath", "//video[@id='index_video']//source")
      , ("video.file.url.xpath", "./@src")
      , ("video.file.type.xpath", "./@type")
  ))
  
  
  private[this] var regexes = Map[String, Regex]() 
  
  case class MatchException(regex: String, str: String) extends RuntimeException {
    override def getMessage: String = "Fehler: Regulärer Ausdruck '"+regex+"' matcht nicht auf '"+str+"'."
  }
  
  def getText(property: String, node:N = null)(implicit doc: Document): String = {
    //String mittels XPath aus Document holen
    val prop = Prop(property+".xpath")
    val str = (if(node == null) xpath(prop) else node.xpath(prop)).getTextContent
    
    //schaue ob eine optionale regex Property exisitert
    Prop.get(property+".regex") match {
      case Some(value) =>
        regexes.synchronized{
          if(!regexes.contains(str))
            regexes += property -> value.r
        }
        val r = regexes(property)
        
        r.findFirstMatchIn(str) match {
          case Some(m) => m.group(1)
          case None => throw MatchException(value, str)
        }
      case None => 
        str
    }    
  }
  
  
  
  def getNodes(property: String, node:N = null)(implicit doc: Document): NL = 
    if(node == null)
      xpath(Prop(property+".xpath"))
    else
      node.xpath(Prop(property+".xpath"))
  
  
  
  val client = AsyncHTTP
  val latestVideo = Video.latest

  type Cond = Video => Boolean
  
  def full = viewPages (_ => true)(_ => false)
  
  def small = viewPages (_.id > latestVideo)(_.id <= latestVideo)
  
  def update(url:String) = {
      val cond:Cond = {v => url.equalsIgnoreCase(v.url)}
      viewPages(cond)(cond)
  }
  
  def viewPages(examine: Cond)(abort: Cond): Unit = 
    de.blackpinguin.util.Time.measureAndPrint{
      val videos = Coll[Future[Video]]()
      val future = examinePage(1, videos)(examine)(abort)
      waitFor(future)
      println(videos.size + " neue Videos")
      videos.foreach(waitFor)
      XML.save
    }

  def examinePage(n: Int, videos: Coll[Future[Video]])(examine: Cond)(abort: Cond): Future[Unit] = {
    for (doc <- client.getDOM(Prop("domain") + Prop("pages.url") + n)) yield {
      implicit val d = doc
      println("Seite " + n + " geladen.")
      var ende = false

      //alle <li>-Elemente parallel auswerten
      val vs = for (li <- getNodes("videos")) yield initVideo(li)
      
      //für alle Videos
      for (fv <- vs){
        //warte auf das Future-Ergebnis
        val v = result(fv)
        //wenn es ein neues video ist
        if (examine(v))
          //betrachte es genauer
          videos.add(examineVideo(v))
        //bei alten Videos nicht zur nächsten Seite
        if(abort(v))
          ende = true
      }
          
      //existiert überhaupt eine weitere Seite?
      if(!ende){
        ende = true
        for(node <- getNodes("pages")){
          if(node.attr.equals(Prop("pages.url")+(n+1)))
            ende = false
        }
      }
      
      if(!ende)
        //nächste Seite laden
        waitFor(examinePage(n + 1, videos)(examine)(abort))
    }
  }
  
  
  //Eingabe Node: <li>
  //hole Infos: url, title, duration 
  def initVideo(node: N)(implicit doc: Document): Future[Video] =
    Future {

      //Video-URL
      val v = Video(Prop("domain") + getText("video.url", node))

      //Video-Titel
      v.title = fixUmlaute(getText("video.title", node))

      //Video-Autor
      v.author = getText("video.author", node)
      
      //Video-Dauer
      v.duration = getText("video.duration", node)
      
      v
    }

  //bekannt: url, title, duration
  //benoetigt: files, pubDate
  def examineVideo(video: Video): Future[Video] = 
    for {
      doc <- client.getDOM(video.url)
    } yield {
      implicit val d = doc

      //println("Video " + video.title + " geladen.")
      
      //Hochladedatum
      video.pubDate = getText("video.pubDate")

      //für alle Videodateien
      getNodes("video.files").foreach { source =>
        //zum Video hinzufügen
        video add VFile(Prop("domain") + source("src"), source("type"))
      }

      //Video zu <videos> hinzufügen
      val addToVideos = Future(XML.add(video))

      //layers ermitteln und zum <index> hinzufügen
      val addLayers = Future(ConfigEntry.layers(video))
      
      waitFor(addToVideos)
      waitFor(addLayers)
      
      video
    }
    

  private lazy val umlaute = Map[String, String](
    ("&Atilde;&Yuml;", "ß"), //sz
    ("&Atilde;&frac14;", "ü"), //uuml
    ("&Atilde;&curren;", "ä"), //auml
    ("&Atilde;&para;", "ö"), //ouml
    ("&Atilde;&oelig;", "Ü"), //Uuml
    ("&Atilde;&bdquo;", "Ä"), //Auml
    ("&szlig;", "ß"),
    ("&uuml;", "ü"),
    ("&auml;", "ä"),
    ("&ouml;", "ö"),
    ("&Uuml;", "Ü"),
    ("&Auml;", "Ä"),
    ("&Ouml;", "Ö") )

  //Umlaute im Titel ersetzen (sonst wird das & bei der Ausgabe zu &amp;)
  def fixUmlaute(in: String) = {
    var out = in
    for (fix <- umlaute)
      out = out.replace(fix._1, fix._2)
    out
  }
  
 
}