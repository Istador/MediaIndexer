package de.blackpinguin.mediaindexer

import collection.immutable.{ SortedSet => Set, SortedMap => Map }

import java.io.File

import de.blackpinguin.util.DOM
import de.blackpinguin.util.DOM._
import de.blackpinguin.util.Dates._

object XML {

  implicit val file = new File("videos.xml")

  if (!file.exists()) {
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
      for (child <- lay.layers)
        addLay(child)
    }

    //index löschen
    while (indexNode.hasChildNodes)
      indexNode.removeChild(indexNode.getFirstChild)

    //index neu erstellen
    for (lay <- RootLayer.layers)
      addLay(lay)

    //Erstellungszeitpunkt
    xpath("/indexer[1]/@gendate").attr = DateTime()

    //ID des neuesten Video
    xpath("/indexer[1]/videos[1]/@latest").attr = Video.latest

    DOM.save(file)

    println("videos.xml updated.")
  }

  def remove(url: String): Unit = this.synchronized {
    val video: N = xpath("/indexer[1]/videos[1]/video[@url='" + url + "']")
    if (video == null) println("Warning: URL " + url + " is not present")
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

      println("removing URL " + url + " at " + vrefs.size + " places")

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
      var self: N = xpath(lay.xpath)
      if (self == null) {
        self = doc.createElement("layer")
        //Name
        self.attr("name", lay.name)
        //Checkbox
        if (lay.checkbox)
          self.attr("checkbox", "true")
        //Gesamtdauer
        if (lay.duration != null)
          self.attr("duration", lay.duration)
        //Anzahl Videos
        if (lay.videos.size > 0)
          self.attr("videos", lay.videos.size)
        parent.appendChild(self)
        //println("Layer: "+lay.name)
      }
    }
  }

  def add(vref: VRef, lay: Layer): Unit = {
    val layerNode: N = xpath(lay.xpath)
    val vrefNode: N = doc.createElement("vref")
    vrefNode.attr("id", vref.id)
    vrefNode.attr("title", vref.title)
    layerNode.appendChild(vrefNode)
    //println("Video: "+vref.title)
  }

  def add(vl: Video): Unit = {
    var nodes: NL = xpath("/indexer[1]/videos[1]/video[@url='" + vl.url + "']")

    //<video>-Element erstellen, falls noch nicht vorhanden
    val node: N =
      if (nodes.size == 0) doc.createElement("video")
      else nodes

    node.attr("id", vl.id)
    node.attr("url", vl.url)
    node.attr("date", vl.date)
    node.attr("pubdate", vl.pubDate)
    node.attr("author", vl.author)
    if (vl.duration != null)
      node.attr("duration", vl.duration)

    //lösche alle <file>-Kindelemente
    while (node.hasChildNodes)
      node.removeChild(node.getFirstChild)

    //füge <file>-Elemente hinzu
    vl.files.foreach { vf =>
      val nFile = doc.createElement("file")
      nFile.attr("url", vf.url)
      nFile.attr("type", vf.typ)
      node.appendChild(nFile)
    }

    //wenn <video> noch nicht vorhanden war, hinzufügen zu <videos>
    if (nodes.size == 0) videosNode.synchronized {
      videosNode.appendChild(node)
    }
  }

}