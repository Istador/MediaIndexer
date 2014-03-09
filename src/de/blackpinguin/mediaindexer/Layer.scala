package de.blackpinguin.mediaindexer

import scala.concurrent.duration.Duration

case class VRef(id:Int, title:String)

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
  
  //alle direkten <layer> Kinder in <index>
  def init = 
    for(node <- XML.indexNode.xpath("./layer")){
      loadNode(node)
    }
  
}


object RootLayer extends Layer(None, "", false) {
  override protected val self: Option[Layer] = None
}

class Layer(val parent: Option[Layer] = None, val name: String, val checkbox: Boolean = false) {

  var duration: Duration = null
  
  //child Layers
  var layers = Array[Layer]()
  var layernames = Map[String, Layer]()

  //child Layers hinzufügen
  protected[Layer] def add(lay: Layer) = {
    this.synchronized {
      layers :+= lay
      layers = layers.sorted
      layernames += lay.name -> lay
    }
  }

  protected val self: Option[Layer] = Some(this)
  
  //child Layers abfragen und notfalls erstellen
  def getLayer(lay: (String, Boolean)): Layer = getLayer(lay._1, lay._2)
  def getLayer(name: String, cb: Boolean = false): Layer =
    if(name == null)
      null
    else if (layernames.contains(name)) 
      layernames(name)
    else {
      val lay = new Layer(self, name, cb)
      add(lay)
      lay
    }

  //natürliche Ordnung für Layers
  lazy val vOrder = Layer.vOrder(this)

  //child Videos
  var videos = Array[VRef]()
  var titles = Map[Int, VRef]()
  def title(v: Video): Option[String] = titles.get(v.id).map{_.title}

  protected[Layer] def add(vref: VRef): Unit = this.synchronized {
    if(!titles.contains(vref.id)){
      videos :+= vref
      videos = videos.sorted(vOrder)
      titles += vref.id -> vref
      //println(name+": "+videos.map(_.title).mkString(", "))
    }
  }
  
  //child Video hinzufügen
  def add(v: Video, title: String): Unit = {
    val vref = VRef(v.id, title)
    //Videodauer aufaddieren
    if(!titles.contains(vref.id)){
      if(duration == null) duration = v.duration
      else duration += v.duration
    }
    add(vref)
  }

  //vollständiger Pfad dieses Layers
  lazy val path: Array[String] = parent match {
    case Some(p) => p.path :+ name
    case None => Array(name)
  }

  lazy val xpath: String =
    "/indexer[1]/index[1]/layer[@name='" + path.mkString("']/layer[@name='") + "']"

}