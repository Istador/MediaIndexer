package de.blackpinguin.mediaindexer

import collection.mutable.{Set => Coll}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.w3c.dom.Document
import scala.util.matching.Regex
import org.apache.commons.lang3.StringEscapeUtils

import de.blackpinguin.util._
import de.blackpinguin.util.{Properties => Prop}
import de.blackpinguin.util.DOM._

object Mediathek {
  
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
      , ("video.comments.xpath", "//div[@id='media_comments_list']/div[1]/div[1]/h2[1]")
      , ("video.comments.regex", "(\\d+) Kommentare")
  ))
  
  
  private[this] var regexes = Map[String, Regex]() 
  
  case class MatchException(regex: String, str: String) extends RuntimeException {
    override def getMessage: String = "Fehler: Regulärer Ausdruck '"+regex+"' matcht nicht auf '"+str+"'."
  }
  
  def getText(property: String, node:N = null)(implicit doc: Document): String = {
    //String mittels XPath aus Document holen
    val prop = Prop(property+".xpath")
    val nl = if(node == null) xpath(prop) else node.xpath(prop)
    if(nl == null) return ""
    val str = nl.getTextContent
    
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
  lazy val aTrue: Any => Boolean = _ => true //immer wahr
  lazy val aFalse: Any => Boolean = _ => false //immer falsch
  
  //von vorne, alle videos überprüfen ID's behalten
  def full = viewPages (aTrue) (aFalse) (1) (_+1) (aFalse)
  
  //von vorne, alle videos überprüfen ID's behalten, bis zur Seite n
  def full(n: Int) = viewPages (aTrue) (aFalse) (1) (_+1) (_ >= n)
  
  //von vorne, abbruch wenn bereits bekannt
  def small = viewPages (_.id > latestVideo) (_.id <= latestVideo) (1) (_+1) (aFalse)
  
  //von hinten, alle videos überprüfen ID's behalten
  def backwards = viewPages (aTrue) (aFalse) (lastPageNumber) (_-1) (_ <= 0)
  
  //lade die letzte Seite, und ermittel die Seitenzahl der Seite.
  def lastPageNumber: Long = {
    implicit val doc = result(client.getDOM(Prop("domain") + Prop("pages.url") + Long.MaxValue))
    (getNodes("pages"):NL).map{ node =>
      val href = node.attr
      //seitenzahl aus der url herausholen
      href.substring(href.lastIndexOf("/")+1).toLong
    }.foldLeft(Long.MinValue){ (max, n) =>
      //wenn diese seite größer als das momentane maximum ist
      if(n > max) n
      else max
    }
  }
  
  //von vorne, solange bis eine bestimmtes Video gefunden wird, nur dieses prüfen
  def update(url: String): Unit = {
      val cond:Cond = {v => url.equalsIgnoreCase(v.url)}
      viewPages (cond) (cond) (1) (_+1) (aFalse)
  }
  
  //updaten anhand der ID
  def update(id: Int): Unit = {
    //hole die url für diese id
    val node = xpath("/indexer[1]/videos[1]/video[@id='" + id + "']")(XML.doc)
    if(node == null) println("Fehler: kann kein Video für die ID '"+id+"' finden.")
    else update( node.attr("url").attr )
  }
  
      
  def hasPage(n: Long)(implicit doc: Document): Boolean = 
    getNodes("pages").exists(_.attr.equals(Prop("pages.url")+n))
  
  
  def viewPages(examine: Cond)(abort: Cond)(firstPage: =>Long)(nextPage: Long=>Long)(lastPage: Long=>Boolean): Unit = {
      val videos = Coll[Future[Video]]()
      val future = examinePage(firstPage, videos)(examine)(abort)(nextPage)(lastPage)
      waitFor(future)
      println(videos.size + " Videos aktualisiert.")
      videos.foreach(waitFor)
      XML.save
    }

  def examinePage(n: Long, videos: Coll[Future[Video]])(examine: Cond)(abort: Cond)(nextPage: Long=>Long)(lastPage: Long=>Boolean): Future[Unit] = {
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
        if(!ende && abort(v))
          ende = true
      }
          
      //existiert überhaupt eine weitere Seite?
      if(!ende && !lastPage(n) && hasPage(nextPage(n)))
        //nächste Seite laden
        waitFor(examinePage(nextPage(n), videos)(examine)(abort)(nextPage)(lastPage))
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
      
      //Kommentaranzahl
      
      val old = video.comments
      video.comments = getText("video.comments").toInt
      //falls sich die Anzahl geändert hat
      if(old != video.comments){
        video.commentsChanged = video.comments - old
      }
      

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
  
  
  //Umlaute im Titel ersetzen (sonst wird das & bei der Ausgabe zu &amp;)
  def fixUmlaute(in: String) = {
    StringEscapeUtils.unescapeHtml4(in)
  }
  
 
}