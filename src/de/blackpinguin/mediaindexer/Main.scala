package de.blackpinguin.mediaindexer

import java.io.File
import de.blackpinguin.util._

object Main {

  def main(args: Array[String]): Unit = {
    val (_, time) = Time.measure{ submain(args) }
    println("Gesamtdauer: "+Time.toStr(time))
  }
  
  def submain(args: Array[String]): Unit = {
    Properties.load(new File("user.conf"))

    if (args.length == 0) {
      smart
    } else if (args.length == 1) {
      args(0) match {
        case "help" => printUsage
        case "-help" => printUsage
        case "--help" => printUsage
        case "h" => printUsage
        case "-h" => printUsage
        case "--h" => printUsage

        case "-small" => small
        case "-smart" => smart
        case "-full" => full
        case "-xslt" => xsltAlone

        case _ => printUsage
      }
    } else if (args.length == 2) {
      args(0) match {
        case "-update" => update(args(1))
        case "-remove" => remove(args(1))
        case _ => printUsage
      }
    } else
      printUsage
  }

  def printUsage = {
    println("MediaIndexer\t\t\tsmal run")
    println("MediaIndexer -small\t\tsearch for new videos, abort on an old video.")
    println("MediaIndexer -smart\t\tgenerate the index again for old videos and make a small search.")
    println("MediaIndexer -full\t\tupdate all videos, keeping only the IDs.")
    println("MediaIndexer -reset\t\tdelete videos.xml and search from last to first page.")
    println("MediaIndexer -xslt\t\tonly transforms videos.xml using the XSLT files")
    println("MediaIndexer -update URL\tupdate a single video")
    println("MediaIndexer -update ID\tupdate a single video")
    println("MediaIndexer -remove URL\tremove a single video")
    println("MediaIndexer -remove ID\tremove a single video")
  }

  //verwende index und video aus xml datei, suche nach neuen videos, breche ab bei bekannten videos
  def small {
    Aktion("Index laden", "aus der XML-Datei") := 
      Layer.init
    Aktion("Suche neue Videos", "solange bis ein altes gefunden wird") := 
      Mediathek.small
    xslt
  }

  def smart {
    
    import XML.doc
    import DOM._

    Aktion("Index neu generieren", "ermittelt für alle alten Videos die Layer neu") := {
      for (node <- XML.videosNode.getChildNodes) {
        //Video-Objekt aus den Daten in der XML-Datei erstellen
        val video = Video.fromXML(node)
        //layers ermitteln und zum <index> hinzufügen
        ConfigEntry.layers(video)
      }
    }
    
    Aktion("Suche neue Videos", "solange bis ein altes gefunden wird") :=
      Mediathek.small
    
    xslt
  }

  //verwende video id's aus xml datei, update alle videos, generiere index neu.
  def full {
    Aktion("Aktuallisiere alle Videos", "behält die IDs") :=
      Mediathek.full
    
    xslt
  }

  //lösche xml datei und erstelle alles neu. traversiere die seiten von der letzten zur ersten.
  def reset {
    //XML-Datei löschen falls vorhanden
    Aktion("Lösche XML-Datei") := {
      val f = new File("videos.xml")
      if (f.exists)
        f.delete
    }
    
    //volle suche von hinten nach vorne, damit die ID's grob richtig geordnet sind
    Aktion("Suche neue Videos", "vollständig, von letzter zur ersten Seite") :=
      Mediathek.backwards
      
    xslt
  }

  def xsltAlone {
    Aktion("Index laden", "aus der XML-Datei") := 
      Layer.init
    RootLayer.needChange
    xslt
  }
  
  /*
   * erstellt aus der bereits vorhandenen xml datei mittels xslt dateien den output neu.
   * die mediathek wird nicht mit http requests belästigt
   */
  def xslt {
    Aktion("Generiere Output mittels XSLT") := {
      val xmlFile = XSLT()
      val layers = XSLT.forEachLayer
      waitFor(xmlFile)
      waitFor(layers)
    }
  }

  /*
   * aktualisiert ein einzelnes video.
   * dabei wird die mediathek seite für seite durchsucht, um auch eine veränderte 
   * dauer mitzubekommen.
   */
  def update(str: String) {
    Aktion("Aktuallisiere ein einzelnes Video") :=
      intOrString(str)(Mediathek.update)(Mediathek.update)
    
    xslt
  }

  /*
   * entfernt ein einzelnes video komplett aus der xml-datei
   */
  def remove(str: String) {
    Aktion("Entferne ein einzelnes Video") :=
      intOrString(str)(XML.remove)(XML.remove)
    
    xslt
  }

  
  def intOrString(str: String)(intOp: Int => Unit)(strOp: String => Unit) {
    try {
      val id = str.toInt
      //wenn die Eingabe ein Integer ist
      intOp(id)
    } catch {
      case _: java.lang.NumberFormatException =>
        //wenn die Eingabe kein Integer ist
        strOp(str)
    }
  }

}