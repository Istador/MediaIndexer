package de.blackpinguin.mediaindexer

import scala.concurrent.duration.Duration
import org.w3c.dom.Document
import de.blackpinguin.util.DOM._
import java.net.URLEncoder
import scala.collection.mutable.ArrayBuffer
import de.blackpinguin.util.ArrayInsertSort

case class VRef(id:Int, title:String){
  def toXML(implicit doc: Document): N = {
    val node: N = doc.createElement("vref")
    node.attr("id", id)
    node.attr("title", title)
    node
  }
}

object Layer {
  
    //natürliche Ordnung für Layers
  implicit object layordering extends Ordering[Layer] {
    def compare(a: Layer, b: Layer) =
      a.name compare b.name
  }

  //natürliche Ordnung für Videos eines Layers
  def vOrder(lay: Layer) = new Ordering[VRef] {
    def compare(a: VRef, b: VRef) =
      a.title compare b.title
  }
  
  //root Layers abfragen und notfalls erstellen
  def getLayer(lay: (String, Boolean)): Layer = getLayer(lay._1, lay._2)
  def getLayer(name: String, cb: Boolean = false): Layer = 
  	RootLayer.getLayer(name, cb)

  

  
  import XML.doc
  import de.blackpinguin.util.DOM._
  
  //aka. fromXML
  private[this] def loadNode(node: N, parent: Option[Layer] = None): Unit = {
    val name = node.attr("name").attr
    val checkbox = { 
      val x = node.attr("checkbox")
      if(x == null) false
      else x.attr.toBoolean
    }
    
    val lay = new Layer(parent, name, checkbox)
    
    val duration = node.attr("duration")
    if(duration != null) lay.duration = duration.attr
    
    val comments = node.attr("comments")
    if(comments != null) lay.comments = comments.attr.toInt
    
    parent match {
      case Some(p) => p.add(lay)
      case None => RootLayer.add(lay)
    }
    
    //<vref>-Kinder von <layer>
    for(vrefNode <- node.xpath("./vref")){
      val id = vrefNode.attr("id").attr
      val title = vrefNode.attr("title").attr
      val vref = VRef(id.toInt, title)
      lay.add(vref)
    }
    
    //alle <layer> Kindelemente
    for(child <- node.xpath("./layer"))
      loadNode(child, Some(lay))
  }
  
  //lade den vorhandenen index ein 
  def init = 
    //alle direkten <layer> Kinder in <index>
    for(node <- XML.indexNode.xpath("./layer")){
      loadNode(node)
    }
  
}


object RootLayer extends Layer(None, "", false) {
  override protected val self: Option[Layer] = None
}

class Layer(val parent: Option[Layer] = None, val name: String, val checkbox: Boolean = false) extends Iterable[Layer] {

  //Name that can be used in a url or filename. can't contain %, because this would be interpreted by the browser
  lazy val url = URLEncoder.encode(name.replace(" ", "_"), "UTF-8").replace("%","+")
  
  var changed: Boolean = false
  
  def needChange: Unit = {
    changed = true
    for(lay <- this)
      lay.needChange
  }
  
  def hasChanged: Unit = if(!changed) {
    changed = true
    parent match {
      case Some(p) => p.hasChanged
      case None => 
    }
  }
  
  
  var duration: Duration = null
  var comments: Int = 0
  
  //child Layers
  val layers = ArrayBuffer[Layer]()
  var layernames = Map[String, Layer]()
  
  def iterator = layers.iterator
  
  //child Layers hinzufügen
  protected[Layer] def add(lay: Layer) = {
    this.synchronized {
      layers.insertSorted(lay)
      layernames += lay.name -> lay
    }
  }

  protected val self: Option[Layer] = Some(this)
  
  //child Layers abfragen und notfalls erstellen
  def getLayer(lay: (String, Boolean)): Layer = getLayer(lay._1, lay._2)
  def getLayer(name: String, cb: Boolean = false): Layer = {
    hasChanged
    if(name == null)
      null
    else if (layernames.contains(name)) 
      layernames(name)
    else {
      val lay = new Layer(self, name, cb)
      add(lay)
      lay
    }
  }

  //natürliche Ordnung für Layers
  lazy val vOrder = Layer.vOrder(this)

  //child Videos
  val videos = ArrayBuffer[VRef]()
  var titles = Map[Int, VRef]()
  def title(v: Video): Option[String] = titles.get(v.id).map{_.title}

  protected[Layer] def add(vref: VRef): Unit = this.synchronized {
    if(!titles.contains(vref.id)){
      videos.insertSorted(vref)(vOrder)
      titles += vref.id -> vref
      //println(name+": "+videos.map(_.title).mkString(", "))
    }
  }
  
  //child Video hinzufügen
  def add(v: Video, title: String): Unit = {
    hasChanged
    val vref = VRef(v.id, title)
    //wenn noch nicht vorhanden
    if(!titles.contains(vref.id)){
      //Videodauer aufaddieren
      if(duration == null) duration = v.duration
      else duration += v.duration
      //Kommentare aufaddieren
      comments += v.comments
    }
    add(vref)
  }
  
  //enhält dieser Layer, oder eines deren Kinder, ein Video mit der ID?
  def containsVideoID(id: Int): Boolean = 
    titles.keySet.contains(id) || exists(_.containsVideoID(id))

  //vollständiger Pfad dieses Layers
  lazy val path: Array[String] = parent match {
    case Some(p) => p.path :+ name
    case None => Array(name)
  }
  
  //vollständiger URL-Pfad dieses Layers
  lazy val urlpath: Array[String] = parent match {
    case Some(p) => p.urlpath :+ url
    case None => Array(url)
  }

  
  lazy val xpath: String = "/indexer[1]/index[1]/layer[@name='" + path.mkString("']/layer[@name='") + "']"
  
  
  def toXML(implicit doc: Document): N = {
    val node: N = doc.createElement("layer")
    
    node.attr("name", name)
    node.attr("url", url)
    if (checkbox)
      node.attr("checkbox", "true")
    if (duration != null)
      node.attr("duration", duration)
    if (comments > 0)
      node.attr("comments", comments)
    if (videos.size > 0)
      node.attr("videos", videos.size)
    
    node
  }
    
}