package de.blackpinguin.util

import org.w3c.dom.{Document, Node, NodeList}
import java.io.{File, InputStream, FileInputStream, OutputStream, FileOutputStream}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.{DOMSource, DOMResult}
import javax.xml.transform.stream.StreamResult
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants.NODESET
import javax.xml.xpath.XPathFactory

object DOM {

  import scala.language.implicitConversions
  
  def open(file: File): Document = 
    open(new FileInputStream(file))
  
  def open(is:InputStream): Document = {
    val factory = DocumentBuilderFactory.newInstance
    val builder = factory.newDocumentBuilder
    builder.parse(is)
  }
  
  def save(out: File)(implicit doc:Document):Unit = {
    //temporäre Datei erstellen
    val tmp = new File(out.getParentFile, out.getName + ".tmp")
    if(tmp.exists) tmp.delete
    tmp.createNewFile
    
    //speichern
    val fos = new FileOutputStream(tmp) 
    save(fos)
    fos.close
    
    if(out.exists) out.delete //output datei löschen
    tmp.renameTo(out) //umbenennen
  }
  
  private[this] def save(os:OutputStream)(implicit doc: Document):Unit = {
    val transformer = TransformerFactory.newInstance.newTransformer
	val source = new DOMSource(doc)
	val result = new StreamResult(os)
	transformer.transform(source, result)
  }
  
  
  lazy val xpfactory = ThreadSafe{this.synchronized{XPathFactory.newInstance}}
  
  def xpath(str: String)(implicit doc: Document):NL = {
    val xp = xpfactory.get.newXPath
    doc.synchronized{
      xp.evaluate(str, doc, NODESET).asInstanceOf[NodeList]
    }
    //val expr = xp.compile(str)
    //expr.evaluate(doc, NODESET).asInstanceOf[NodeList]
  }
  
  
  
  private[DOM] def xpath(str: String, n:Node)(implicit doc: Document):NL = {
    val xp = xpfactory.get.newXPath
    n.synchronized {
      xp.evaluate(str, n, NODESET).asInstanceOf[NodeList]
    }
  }
  
  
  implicit class ExtendedDocument(doc: Document){
    
    private[this] val tfact = ThreadSafe(TransformerFactory.newInstance)
    
    def copy: Document = {
      val tx = tfact.get.newTransformer
      val source = new DOMSource(doc)
      val result = new DOMResult()
      tx.transform(source, result)
      result.getNode.asInstanceOf[Document]
    } 
  }
  
  
  case class N(n: Node)(implicit doc: Document){
    def attr = if(n != null) n.getNodeValue() else null
    def attr_=(str: String) = n.setNodeValue(str)
    
    def attr(key: String):N = {
      val atts = n.getAttributes()
      if(atts == null) null
      atts.getNamedItem(key)
    }
    
    def apply(key: String):String = attr(key).attr
    
    
    def attr(key: String, value: String):Unit = {
      val ref = attr(key)
      
      if(ref == null || ref.n == null){
    	  val a = doc.createAttribute(key)
    	  a.setValue(value)
    	  n.getAttributes.setNamedItem(a)
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
  
  case class NL(nl: NodeList)(implicit doc: Document) extends Seq[N]{
    
    def apply(index: Int): N = if(nl == null) null else nl.item(index)
    
    def iterator: Iterator[N] = new Iterator[N]{
      private[this] var n = 0 
      def hasNext: Boolean = n < nl.getLength
      def next: N = { n=n+1 ; apply(n-1) }
    }
    
    def length: Int = if(nl == null) 0 else nl.getLength()
    
    /*
    def foreach[T](f: N => T):Unit = if(nl != null){
      for(i <- (0 until size)){
        f(nl.item(i))
      }
    }
    
    def forall(f: N => Boolean): Boolean = {
      for(n <- this) 
        if(!f(n)) 
          return false
      true
    }
    
    def exists(f: N => Boolean): Boolean = {
      for(n <- this)
        if(f(n))
          return true
      false
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
    */
    
  }
  implicit def NodeList2NL(nl: NodeList)(implicit doc: Document): NL = 
    if(nl == null) null else NL(nl)
  implicit def NL2NodeList(nl: NL):NodeList = nl.nl
  implicit def NL2Node(nl: NL):Node = nl.n
  implicit def NL2N(nl:NL):N = nl(0)
  
}