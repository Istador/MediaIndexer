package de.blackpinguin.mediaindexer

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{StreamSource, StreamResult}
import java.io.{File, FileOutputStream, ByteArrayOutputStream, ByteArrayInputStream}
import org.w3c.dom.Document
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import de.blackpinguin.util._
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
    	throw new RuntimeException("Fehler: '"+parent.getPath+"' ist kein Ordner.")
    if(!dir.exists)
      if(!dir.mkdir)
        println("Fehler: kann Ordner '"+dir.getPath+"' nicht erstellen")
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
      //temporäre Datei erstellen
      val tmp = new File(out.getParentFile, out.getName + ".tmp")
      if(tmp.exists) tmp.delete
      tmp.createNewFile
      
      //transformieren
      val tFactory = TransformerFactory.newInstance
      val transformer = tFactory.newTransformer(new StreamSource(xslt))
      val fos = new FileOutputStream(tmp)
      transformer.transform(xml, new StreamResult(fos))
      fos.close //Datei schließen (Wichtig, sonst kein renameTo !)
      
      if(out.exists) out.delete //output datei löschen
      tmp.renameTo(out) //tmp datei umbenennen
      
      println(out.getPath + " generated.")
    } catch {
      case _:Throwable => throw new RuntimeException("Error: generating '"+out.getPath+"' using '"+xslt.getPath+"'.")
    }
  }
  
  
}