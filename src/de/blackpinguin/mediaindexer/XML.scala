package de.blackpinguin.mediaindexer

import collection.immutable.{ SortedSet => Set, SortedMap => Map }

import java.io.File

import de.blackpinguin.util.DOM
import de.blackpinguin.util.DOM._
import de.blackpinguin.util.Dates._

object XML {

  implicit val file = new File("videos.xml")
  
  if (!file.exists) {
    val xml =
      <indexer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="videos.xsd" gendate={ DateTime() }>
        <index/>
        <videos latest="0"/>
      </indexer>
    scala.xml.XML.save(file.getName, xml, "UTF-8", true, scala.xml.dtd.DocType("indexer", scala.xml.dtd.SystemID("videos.dtd"), Nil))
  }

  implicit lazy val doc = DOM.open(file)
  
  val indexNode: N = xpath("/indexer[1]/index[1]")
  val videosNode: N = xpath("/indexer[1]/videos[1]")

  def save = this.synchronized {
    println("updating videos.xml...")

    //funktion um rekursiv Layer hinzuzufügen
    def addLay(lay: Layer): Unit = {
      add(lay)
      for (vref <- lay.videos)
        add(vref, lay)
      for (child <- lay)
        addLay(child)
    }

    //index löschen
    while (indexNode.hasChildNodes)
      indexNode.removeChild(indexNode.getFirstChild)

    //index neu erstellen
    for (lay <- RootLayer)
      addLay(lay)

    //Erstellungszeitpunkt
    xpath("/indexer[1]/@gendate").attr = DateTime()

    //ID des neuesten Video
    xpath("/indexer[1]/videos[1]/@latest").attr = Video.latest

    DOM.save(file)

    println("videos.xml updated.")
  }
  
  def remove(id: Int): Unit = 
    remove(xpath("/indexer[1]/videos[1]/video[@id='" + id + "']"))
  
  def remove(url: String): Unit = 
    remove(xpath("/indexer[1]/videos[1]/video[@url='" + url + "']"))
  
  private[this] def remove(video: N): Unit = {
    
    if (video == null) println("Warnung: dieses Video existiert nicht.")
    else {
      //hole die id des Videos das zur url gehört
      val id = video.attr("id").attr

      //entferne aus <videos>
      val vparent = video.getParentNode
      vparent.synchronized {
        vparent.removeChild(video)
      }

      //alle <vref>'s auf das Video finden
      val vrefs: NL = xpath("//vref[@id=" + id + "]")

      println("removing video at " + vrefs.size + " places")

      //alle <vref>'s entfernen
      for (v <- vrefs) {
        val refparent = v.getParentNode
        refparent.synchronized { refparent.removeChild(v) }
      }
    }
  }

  def add(lay: Layer): Unit = {
    val parent: N = lay.parent match {
      case Some(p) => xpath(p.xpath)
      case None => indexNode
    }

    parent.synchronized {
      val self: N = xpath(lay.xpath)
      if (self == null) {
        parent.appendChild(lay.toXML)
        //println("Layer: "+lay.name)
      }
    }
  }

  def add(vref: VRef, lay: Layer): Unit = {
    val layerNode: N = xpath(lay.xpath)
    layerNode.appendChild(vref.toXML)
    //println("Video: "+vref.title)
  }

  def add(vl: Video): Unit = {
    var node: N = xpath("/indexer[1]/videos[1]/video[@url='" + vl.url + "']")
    
    //loeschen falls bereits vorhanden
    if(node != null){
      node.synchronized {
        while (node.hasChildNodes)
          node.removeChild(node.getFirstChild)
      }
      videosNode.synchronized {
        videosNode.removeChild(node)
      }
    }

    //<video>-Element erstellen
    node = vl.toXML

    //hinzufügen zu <videos>
    videosNode.synchronized {
      videosNode.appendChild(node)
    }
  }

}