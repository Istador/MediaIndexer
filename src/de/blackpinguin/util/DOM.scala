package de.blackpinguin.util

import org.w3c.dom.{Document, Node, NodeList}
import java.io.{File, InputStream, FileInputStream, OutputStream, FileOutputStream}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants.NODESET


object DOM {

  import scala.language.implicitConversions
  
  def open(file: File): Document = 
    open(new FileInputStream(file))
  
  def open(is:InputStream): Document = {
    val factory = DocumentBuilderFactory.newInstance
    val builder = factory.newDocumentBuilder
    builder.parse(is)
  }
  
  def save(file: File)(implicit doc:Document):Unit = 
    save(new FileOutputStream(file))
  
  def save(os:OutputStream)(implicit doc: Document):Unit = {
    val transformer = TransformerFactory.newInstance().newTransformer();
	val source = new DOMSource(doc);
	val result = new StreamResult(os);
	transformer.transform(source, result);
  }
  
  
  
  def xpath(str: String)(implicit doc: Document):NL = {
    val xp = javax.xml.xpath.XPathFactory.newInstance().newXPath()
    xp.evaluate(str, doc, NODESET).asInstanceOf[NodeList]
    //val expr = xp.compile(str)
    //expr.evaluate(doc, NODESET).asInstanceOf[NodeList]
  }
  
  
  
  private[DOM] def xpath(str: String, n:Node)(implicit doc: Document):NL = {
    val xp = javax.xml.xpath.XPathFactory.newInstance().newXPath()
    xp.evaluate(str, n, NODESET).asInstanceOf[NodeList]
  }
  
  
  
  case class N(n: Node)(implicit doc: Document){
    def attr = if(n != null) n.getNodeValue() else null
    def attr_=(str: String) = n.setNodeValue(str)
    
    def attr(key: String):N = n.getAttributes().getNamedItem(key)
    def apply(key: String):String = attr(key).attr
    
    
    def attr(key: String, value: String):Unit = {
      val ref = attr(key)
      
      if(ref == null){
    	  val a = doc.createAttribute(key)
    	  a.setValue(value)
    	  n.getAttributes().setNamedItem(a)
      } else {
        ref.attr = value
      }
    }
    
    def xpath(str: String)(implicit doc: Document) = 
      DOM.xpath(str, this)
      
  }
  
  implicit def Node2N(n: Node)(implicit doc: Document): N = 
    if(n==null) null else N(n)
  implicit def N2Node(n: N): Node = 
    if(n==null) null else n.n
  
  case class NL(nl: NodeList)(implicit doc: Document){
    def apply(index:Int):N = if(nl == null) null else nl.item(index)
    def size:Int = if(nl == null) 0 else nl.getLength()
    
    def foreach[T](f: N => T):Unit = if(nl != null){
      for(i <- (0 until size)){
        f(nl.item(i))
      }
    }
    
    def map[T](f: N => T):List[T] = {
      var r = List[T]()
      for(i <- (1 to size))
        r = f(nl.item(size-i)) :: r
      r
    }
    
    def flatMap[T](f: N => List[T]):List[T] = {
      var r = List[T]()
      for(i <- (1 to size))
        r = f(nl.item(size-i)) ::: r
      r
    }
    
  }
  implicit def NodeList2NL(nl: NodeList)(implicit doc: Document): NL = 
    if(nl == null) null else NL(nl)
  implicit def NL2NodeList(nl: NL):NodeList = nl.nl
  implicit def NL2Node(nl: NL):Node = nl.n
  implicit def NL2N(nl:NL):N = nl(0)
  
}