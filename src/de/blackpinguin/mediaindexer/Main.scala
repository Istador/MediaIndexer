package de.blackpinguin.mediaindexer

import java.net.{ URL, URLConnection }
import scala.xml._
import collection.immutable.{ SortedSet => Set }

object Main {

  def genHTML() : Unit = {
    //aus datei holen
    val old = Saver.load
    genHTML(old)
  }

  def genHTML(neu: Set[VideoLink]): Unit = {
    //HTML erzeugen
    val html = Output.toHtml(neu)

    //HTML in Datei speichern
    scala.xml.XML.save("index.html", html, "UTF-8", false, xml.dtd.DocType("html", xml.dtd.SystemID("about:legacy-compat"), Nil))
  }

  def index() : Unit = {

    //Videos aus Datei laden
    val old = Saver.load

    //auf server nach neuen Videos nachschauen
    val neu = Mediathek.getVideos(1, old)

    //bei Veränderung
    if (!old.equals(neu)) {
      println("neue videos")

      //Datenbank speichern
      Saver.save(neu.to)

      //HTML generieren
      genHTML(neu)
    } else {
      println("keine neuen videos")
    }
  }
  
}