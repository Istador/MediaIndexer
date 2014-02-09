package de.blackpinguin.mediaindexer

object Layer {
  
  
  //natürliche Ordnung für Layers
  implicit object layordering extends Ordering[Layer] {
    def compare(a: Layer, b: Layer) =
      a.name compare b.name
  }
  
  
  //natürliche Ordnung für Videos eines Layers
  def vOrder(lay:Layer) = new Ordering[Video] {
    def compare(a: Video, b: Video) =
      lay.titles(a) compare lay.titles(b)
  } 
  
  
  //root Layers
  val layers = scala.collection.mutable.ArrayBuffer[Layer]()
  val layernames = scala.collection.mutable.Map[String, Layer]()
  
  //root Layers hinzufügen
  def add(lay: Layer) = {
    layers += lay
    layers.sorted
    layernames += lay.name -> lay
  }
  
  //root Layers abfragen und notfalls erstellen
  def getLayer(lay:(String, Boolean)):Layer = getLayer(lay._1, lay._2)
  def getLayer(lay:String, cb:Boolean = false): Layer = {
    if(layernames.contains(lay)){
      layernames(lay)
    } else {
      val l = Layer(None, lay, cb)
      add(l)
      l
    }
  }
  
  
  
}



case class Layer(parent:Option[Layer] = None, name:String, checkbox:Boolean = false) {
  
  //child Layers
  val layers = scala.collection.mutable.ArrayBuffer[Layer]()
  val layernames = scala.collection.mutable.Map[String, Layer]()
  
  //child Layers hinzufügen
  def add(lay: Layer) = {
    layers += lay
    layers.sorted
    layernames += lay.name -> lay
  }
  
  //child Layers abfragen und notfalls erstellen
  def getLayer(lay:(String, Boolean)):Layer = getLayer(lay._1, lay._2)
  def getLayer(lay:String, cb:Boolean = false): Layer = {
    if(layernames.contains(lay)){
      layernames(lay)
    } else {
      val l = Layer(Some(this), lay, cb)
      add(l)
      l
    }
  }
  
  //natürliche Ordnung für Layers
  lazy val vOrder = Layer.vOrder(this)
  
  //child Videos
  val videos = scala.collection.mutable.ArrayBuffer[Video]()
  val titles = scala.collection.mutable.Map[Video, String]()
  def title(v: Video):Option[String] = titles.get(v)
  
  //child Video hinzufügen
  def add(v: Video, title: String) = {
    videos += v
    videos.sorted(vOrder)
    titles += v -> title
  }
  
  //vollständiger Pfad dieses Layers
  lazy val path: Array[String] = parent match {
    case Some(p) => p.path :+ name
    case None => Array(name)
  }  
  
  
}