package de.blackpinguin.mediaindexer

import de.blackpinguin.mediaindexer.{ VideoLink => VL }
import collection.immutable.{ SortedSet => Set, SortedMap => Map }

import java.io.File
import java.io.FileOutputStream
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node
import org.w3c.dom.NodeList


import rx.lang.scala.Observable

object XML {

  
  implicit val file = new File("videos.xml")
  
  if (!file.exists()){
        val xml = 
<indexer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="videos.xsd" gendate={ date2String(new java.util.Date()) }>
	<index/>
	<videos/>
</indexer>
        scala.xml.XML.save(file.getName, xml, "UTF-8", true, scala.xml.dtd.DocType("indexer", scala.xml.dtd.SystemID("videos.dtd"), Nil))
    }
  
  
  val docFactory = DocumentBuilderFactory.newInstance
  val docBuilder = docFactory.newDocumentBuilder
  implicit val doc = docBuilder.parse(file)
  
  
  
  
  private[this] val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private[this] def date2String(date: java.util.Date): String = {
    sdf.format(date)
  }

  def xpath(str: String):NL = {
    val xp = javax.xml.xpath.XPathFactory.newInstance().newXPath()
    val expr = xp.compile(str)
    expr.evaluate(doc, javax.xml.xpath.XPathConstants.NODESET).asInstanceOf[org.w3c.dom.NodeList]
  }
  
  implicit class N(val n: Node){
    def attr = n.getNodeValue()
    def attr_=(str: String) = n.setNodeValue(str)
    
    def attr(key:String):N = n.getAttributes().getNamedItem(key)
    
    def attr(key:String, value:String):Unit = {
      val ref = attr(key)
      if(ref == null){
    	  val a = doc.createAttribute(key)
    	  a.setValue(value)
    	  n.getAttributes().setNamedItem(a)
      } else {
        ref.attr = value
      }
    }
  }
  
  implicit def N2Node(n: N):Node = n.n
  
  implicit class NL(val nl: NodeList){
    def apply(index:Int):N = nl.item(index)
    def size:Int = nl.getLength()
    
    def foreach[T](f: Node => T):Unit = {
      for(i <- (0 until size))
        f(nl.item(i))
    }
    
    def map[T](f: Node => T):List[T] = {
      var r = List[T]()
      for(i <- (1 to size))
        r = f(nl.item(size-i)) :: r
      r
    }
    
    def flatMap[T](f: Node => List[T]):List[T] = {
      var r = List[T]()
      for(i <- (1 to size))
        r = f(nl.item(size-i)) ::: r
      r
    }
    
  }
  
  implicit def NL2NodeList(nl: NL):NodeList = nl.nl
  implicit def NL2Node(nl: NL):Node = nl.n
  implicit def NL2N(nl:NL):N = nl(0)
  
  def save = {
    xpath("/indexer[1]/@gendate").attr = date2String(new java.util.Date())
    
	val transformer = TransformerFactory.newInstance().newTransformer();
	val source = new DOMSource(doc);
	val result = new StreamResult(new FileOutputStream(file));
	transformer.transform(source, result);
  }
  
  
  
  def remove(url: String): Unit = {
    val video = xpath("/indexer[1]/videos[1]/video[@url='"+url+"']")
    if(video.size == 0) println("Warning: URL "+url+" is not present")
    else if(video.size == 1){
        val id = video.attr("id").attr
    	video.getParentNode().removeChild(video)
    	
    	val vrefs = xpath("//vref[@id="+id+"]")
    	
    	println("removing URL "+url+" at "+vrefs.size+" places")
    	
    	for(v <- vrefs){
    		v.getParentNode().removeChild(v)
    	}
    }
  }
  
  
  def add(vl: Video): Unit = {
    //TODO add link to videos and layers
  }
  
  
  def update(vl: Video): Unit = {
    val video = xpath("/indexer[1]/videos[1]/video[@url='"+vl.url+"']")
    if(video.size == 0) println("Warning: URL "+vl.url+" is not present yet.")
    if(video.size <= 1){
      video.attr("date", vl.date)
      video.attr("pubdate", vl.pubDate)
      if(vl.duration != null)
        video.attr("duration", vl.duration)
      video.getChildNodes().foreach { n => video.removeChild(n) }
      //TODO: add files
    }
  }

  
}