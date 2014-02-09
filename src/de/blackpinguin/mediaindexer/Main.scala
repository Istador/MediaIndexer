package de.blackpinguin.mediaindexer

import java.net.{ URL, URLConnection }
import collection.immutable.{ SortedSet => Set }

object Main {
  
  def main(args: Array[String]): Unit = {
    if(args.length == 0){
      //XML.remove("http://video3/")
      //XML.save
      //full
      //XSLT()
      val v = new Video("url")
      v.title = "M1 2013-01-01 02 Hallo Welt"
      println(ConfigEntry.entries(0).layer(v).path.mkString(", "))
      
    } else if(args.length == 1){
      args(0) match{
        case "help" => printUsage
        case "-help" => printUsage
        case "--help" => printUsage
        case "h" => printUsage
        case "-h" => printUsage
        case "--h" => printUsage
        
        case "-small" => small
        case "-full" => full
        case "-xml" => xml
        case "-xslt" => xslt
        
        case _ => printUsage
      }
    } else if(args.length == 2){
      args(0) match{
        case "-update" => update(args(1))
        case "-remove" => remove(args(1))
        case _ => printUsage
      }
    } else 
      printUsage
  }

  def printUsage = {
    println("MediaIndexer\t\t\tsmal run")
    println("MediaIndexer -small\t\tsmall run")
    println("MediaIndexer -full\t\tfull run")
    println("MediaIndexer -xml\t\tgenerate videos.xml only")
    println("MediaIndexer -xslt\t\ttransform videos.xml using the XSLT files")
    println("MediaIndexer -update URL\tupdate a single video")
    println("MediaIndexer -remove URL\tremove a single video")
  }
  
  
  def small { xml ; xslt }
  
  def full {
    val f = new java.io.File("xml/videos.xml")
    if(f.exists())
      f.delete()
    small
  }
  
  def xml { index() }
  def xslt { XSLT() }
  
  def update(url:String){}
  def remove(url:String){ XML.remove(url) }
  
  def genHTML() : Unit = {
    //aus datei holen
    //val old = Saver.load
    //genHTML(old)
  }

  def genHTML(neu: Set[VideoLink]): Unit = {
    //HTML erzeugen
    val html = Output.toHtml(neu)

    //HTML in Datei speichern
    scala.xml.XML.save("index.html", html, "UTF-8", false, scala.xml.dtd.DocType("html", scala.xml.dtd.SystemID("about:legacy-compat"), Nil))
  }

  def index() : Unit = {
    /*
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
    */
  }
  
}