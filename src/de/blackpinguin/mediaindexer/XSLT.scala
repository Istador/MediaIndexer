package de.blackpinguin.mediaindexer

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{StreamSource, StreamResult}
import java.io.{File, FileOutputStream}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import de.blackpinguin.util._

object XSLT {

  val key = "javax.xml.transform.TransformerFactory"
  val value = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl"
  val props = System.getProperties
  props.put(key, value);
  System.setProperties(props);

  def apply() {
    //XSLT-Ordner
    val dir = new File("xslt")
    if (dir.exists && !dir.isDirectory)
      throw new RuntimeException("Error: './xslt' is not a directory")
    if(!dir.exists)
      dir.mkdir()
    
    //Output-Ordner
    val outdir = new File("output")
    if(outdir.exists && !outdir.isDirectory)
    	throw new RuntimeException("Error: './output' is not a directory")
    if(!outdir.exists)
      outdir.mkdir()
      
    //XML-Datei
    val xml = new File("videos.xml")
    if (!xml.exists || !xml.isFile)
      throw new RuntimeException("Error: missing file './videos.xml'")

    //XSLT-Dateien
    var futures = Set[Future[Unit]]()
    val files = dir.listFiles()
    for (f <- files) {
      if (f.isFile()) {
        val fname = f.getName().toLowerCase()
        val point = fname.lastIndexOf('.')
        val name = fname.substring(0, point)
        val ext = fname.substring(point + 1)
        if (ext.equals("xslt")) {
          println("generate " + name + "...")
          //Parsing
          val future = Future[Unit] {
        	  XSLT(xml, f, new File(outdir, name))
          }
          futures += future
        }
      }
    }
    
    //auf alle Threads warten
    futures foreach waitFor
  }

  
  
  def apply(xml: String, xslt: String, out: String) {
    apply(new File(xml), new File(xslt), new File(out))
  }

  
  
  def apply(xml: File, xslt: File, out: File) {
    try {
      out.delete()
      val tFactory = TransformerFactory.newInstance();
      val transformer = tFactory.newTransformer(new StreamSource(xslt));
      transformer.transform(new StreamSource(xml), new StreamResult(new FileOutputStream(out)));
      println(out.getName + " generated.")
    } catch{
      case _:Throwable => println("Error: generating '"+out.getName+"' from '"+xml.getName+"' using '"+xslt.getName+"'.")
    }
  }

  
  
}