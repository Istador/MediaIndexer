package de.blackpinguin.mediaindexer

import org.w3c.dom.Document

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import de.blackpinguin.util._
import de.blackpinguin.util.DOM._

object SubPages {
  
  //Default: erstelle Subpages
  Properties.addDefault("genSubPages", "true")
  
  def createLayer(lay: Layer, depth: Int, empty: Unit=>Document): Future[Unit] = Future {
    implicit val doc = empty()
    val index: N = xpath("/indexer[1]/index[1]")
    val videos: N = xpath("/indexer[1]/videos[1]")
    
    //diesen und alle Sublayer und VRefs einfügen
    def addLay(layer: Layer, parent: N): Int = {
      val node = layer.toXML
      
      //diesen Layer in den Parent einfügen
      parent.appendChild(node)
      
      //hoechste ID bestimmen
      val max = layer.videos.foldLeft(0){ (max, vref) =>
        //VREF in Layer einfügen
        node.appendChild(vref.toXML)
        if(vref.id > max) vref.id
        else max
      }
      
      layer.foldLeft(max){ (max, child) =>
        //Sublayer einfügen
        val newmax = addLay(child, node)
        if(newmax > max) newmax
        else max
      }
    }
    
    for(i <- 0 until lay.path.length-1)
      index.appendChild{
        val n:N = doc.createElement("parent")
        n.attr("name", lay.path(i))
        n.attr("path", (0 until (lay.path.length - i - 1) ).map(_=>"..").mkString("/") + "/")
        n
      }
    
    val max = addLay(lay, index)
    videos.attr("latest", max.toString)
    
    val relPath = (0 until depth).map(_=>"..").mkString("/") + "/"
    val first = xpath("/indexer[1]/index[1]/layer[1]") 
    first.attr("relPath", relPath)
    first.attr("absPath", lay.path.map(_.replace(" ","_")).mkString("/"))
    
    waitFor(XSLT(doc, lay.path.map{_.replace(" ", "_")}.toList))
  }
  
  def eachLayer: Future[Unit] = Future {
    implicit val doc = XML.doc.copy
    val index: N = xpath("/indexer[1]/index[1]")
    val videos: N = xpath("/indexer[1]/videos[1]")
    while (index.hasChildNodes)
      index.removeChild(index.getFirstChild)
    
    val empty: Unit=>Document = {Unit=>doc.copy}
    
    def allLayers(lay: Layer, depth: Int): Future[Unit] = Future {
      if(!lay.checkbox && lay.changed){
        val f = createLayer(lay, depth, empty)
        val fs = for(child <- lay) yield allLayers(child, depth+1)
        waitFor(f)
        fs foreach waitFor
      }
    }
    
    val fs = for(lay <- RootLayer)
      yield allLayers(lay, 1)
    
    fs foreach waitFor
  }
  
  def apply():Future[Unit] = Future{
    if(Properties("genSubPages").toBoolean)
      waitFor(eachLayer)
  }
  
}