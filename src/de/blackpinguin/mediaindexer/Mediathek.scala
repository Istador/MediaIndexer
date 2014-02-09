package de.blackpinguin.mediaindexer

import scala.xml.NodeSeq
import collection.immutable.{ SortedSet => Set }
import de.blackpinguin.mediaindexer.{ VideoLink => VL }

object Mediathek {

  val domain = "http://media.mt.haw-hamburg.de"
  val div = "div"

  //lädt alle Videos von der Seite und fügt sie zum Set hinzu, falls sie noch nicht vorhanden sind
  def getVideos(page: Int = 1, set: Set[VL] = Set[VL]()): Set[VL] = {
    println("hole Seite " + page)

    //URL die 45 Videos auf einmal lädt
    val url = "/media/list/component/boxList/filter/all/limit/all/layout/thumb/page/"

    //XML-Dokument aus der HTML-Antwort des Servers generieren
    val doc = HTTP.getXML(domain + url + page)

    //XPATH: /html/body[1]/div[3]/div[1]/div[3]/div[2]/div[2]/div[2]/
    val xml = (((((((doc \ "body")(0) \ div)(2) \ div)(0) \ div)(2) \ div)(1) \ div)(1) \ div)(1)

    //Liste aller Videos
    // xml/ul[1]/li		/h3/a
    val list = (xml \ "ul")(0) \ "li"

    //nur neue videos hinzufügen
    val newset = addVideos(list, set)

    //Videos von weiteren Seiten laden
    /// xml/div[1]/div[1]/a
    val pages = ((xml \ div)(0) \ div)(0) \ "a"
    morePages(pages, url, (page + 1), newset)
  }

  //Schaut ob noch weitere Seiten mit Videos existieren, und lädt diese
  private def morePages(pages: NodeSeq, url: String, page: Int, set: Set[VL]): Set[VL] = {
    //für alle verlinkten Seiten
    for (a <- pages) {
      val href = (a \ "@href").text
      //existiert ein Link auf eine Seite mit einen um 1 höheren Index?
      if (href.equals(url + page))
        return getVideos(page, set)
    }
    //keine weiteren Seiten, gebe Eingabe direkt aus
    set
  }

  //fügt die Videos die auf der aktuellen Seite gefunden wurden zum Set hinzu
  private def addVideos(list: NodeSeq, set: Set[VL]) = {
    var s = set
    //für jedes Video
    for (li <- list) {
      val a = ((li \ "h3")(0) \ "a")(0)
      val duration = ((li \ div)(0) \ "span")(0).text
      val title = Output.fixUmlaute((a \ "@title").text)
      val href = domain + (a \ "@href").text
      //wenn der Videotitel valide ist
      if (VL.isValid(title)) {
        val vl = VL(title, href, duration)
        //wenn das Video noch nicht vorhanden ist
        if (!s.contains(vl)) {
          //video url holen
          getVideoLink(vl)
          //zum set hinzufügen
          s = s + vl
        }
      }
    }
    s
  }

  //für Videos die noch nicht abgerufen wurden, lade sie und ermittel die Video-URL
  def getVideoLink(vl: VL) = {
    println("hole Video " + vl.title)

    //HTTP-Request und umwandeln zu XML
    val doc = HTTP.getXML(vl.url)

    //Video-Link aus DOM-Struktur holen, und Dateiendung entfernen
    vl.videolink = domain + (((doc \\ "video")(0) \ "source")(0) \ "@src").text.split("\\.")(0)
  }

}