package de.blackpinguin.mediaindexer

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{StreamSource, StreamResult}
import java.io.{File, FileOutputStream, ByteArrayOutputStream, ByteArrayInputStream}
import org.w3c.dom.Document
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import de.blackpinguin.util._
import de.blackpinguin.util.DOM._
import javax.xml.transform.dom.DOMSource

object XSLT {

  val key = "javax.xml.transform.TransformerFactory"
  val value = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl"
  val props = System.getProperties
  props.put(key, value);
  System.setProperties(props);

  def createDir(name: String, parent: File = new File(".")): File = {
    val dir = new File(parent, name)
    if(dir.exists && !dir.isDirectory)
    	throw new RuntimeException("Error: '"+parent.getPath+"/"+name+"' is not a directory.")
    if(!dir.exists)
      if(!dir.mkdir)
        println("Fehler: kann Ordner '"+dir.getCanonicalPath+"' nicht erstellen")
    dir
  }
  
  //Output-Ordner
  lazy val outDir = createDir("output")
  
  //XSLT-Files
  lazy val xsltFiles = for{
      f <- createDir("xslt").listFiles
      if f.isFile
      fname = f.getName.toLowerCase
      point = fname.lastIndexOf('.')
      name = fname.substring(0, point)
      ext = fname.substring(point + 1)
      if(ext.equals("xslt"))
    } yield (name, f)
  
  def apply(doc: Document, path: List[String], dir: File = outDir): Future[Unit] = {
    if(path.isEmpty)
      Future {
        val fs = for{(name, f) <- xsltFiles} yield 
          Future { XSLT(doc, f, new File(dir, name)) }
        fs foreach waitFor
      }
    else {
      val newDir = createDir(path.head, dir)
      apply(doc, path.tail, newDir)
    }
  }
  
  def apply(): Future[Unit] = {
    //XML-Datei
    val xml = new File("videos.xml")
    if (!xml.exists || !xml.isFile)
      throw new RuntimeException("Error: missing file './videos.xml'")

    //XSLT-Dateien
    Future{
      val fs = for{ (name, f) <- xsltFiles } yield 
        Future { XSLT(xml, f, new File(outDir, name)) }
      fs foreach waitFor
    }
  }

  
  
  def apply(xml: String, xslt: String, out: String): Unit = {
    apply(new File(xml), new File(xslt), new File(out))
  }
  
  def apply(xml: File, xslt: File, out: File): Unit = {
    apply(new StreamSource(xml), xslt, out)
  }
  
  def apply(doc: Document, xslt: File, out: File): Unit = {
    val src = new DOMSource(doc)
    val fact = TransformerFactory.newInstance
    val transf = fact.newTransformer
    val result = new StreamResult()
    val baos = new ByteArrayOutputStream()
    result.setOutputStream(baos)
    transf.transform(src, result)
    val bais = new ByteArrayInputStream(baos.toByteArray)
    apply(new StreamSource(bais), xslt, out)
  }

  def apply(xml: StreamSource, xslt: File, out: File): Unit = {
    try {
      out.delete()
      val tFactory = TransformerFactory.newInstance
      val transformer = tFactory.newTransformer(new StreamSource(xslt))
      transformer.transform(xml, new StreamResult(new FileOutputStream(out)))
      println(out.getPath + " generated.")
    } catch{
      case _:Throwable => println("Error: generating '"+out.getName+"' using '"+xslt.getName+"'.")
    }
  }

  def createLayer(lay: Layer, depth: Int, empty: Unit=>Document): Future[Unit] = Future {
    implicit val doc = empty()
    val index: N = xpath("/indexer[1]/index[1]")
    val videos: N = xpath("/indexer[1]/videos[1]")
    
    def addLay(layer: Layer, parent: N): Unit = {
      val node = layer.toXML
      
      parent.appendChild(node)
      
      for(vref <- layer.videos)
        node.appendChild(vref.toXML)
    
      for(child <- layer)
        addLay(child, node)
    }
    
    for(i <- 0 until lay.path.length-1)
      index.appendChild{
        val n:N = doc.createElement("parent")
        n.attr("name", lay.path(i))
        n.attr("path", (0 until (lay.path.length - i - 1) ).map(_=>"..").mkString("/") + "/")
        n
      }
    
    addLay(lay, index)
    
    val relPath = (0 until depth).map(_=>"..").mkString("/") + "/"
    val first = xpath("/indexer[1]/index[1]/layer[1]") 
    first.attr("relPath", relPath)
    first.attr("absPath", lay.path.map(_.replace(" ","_")).mkString("/"))
    
    waitFor(apply(doc, lay.path.map{_.replace(" ", "_")}.toList))
  }
  
  def forEachLayer: Future[Unit] = Future {
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
  
}