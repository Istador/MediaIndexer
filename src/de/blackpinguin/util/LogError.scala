package de.blackpinguin.util

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

import Dates.DateTime

object LogError {
  
  private[LogError] lazy val file = new File("error.log")
  
  private[LogError] def openCreate(f: File) = {
    if(f.exists && !f.isFile){
      println("Fehler: '"+f.getPath+"' ist keine Datei.")
      throw new RuntimeException("Fehler: '"+f.getPath+"' ist keine Datei.")
    }
    if(!f.exists)
      if(!f.createNewFile){
        println("Fehler: kann Datei '"+f.getPath+"' nicht erstellen.")
        null
      }
    new FileOutputStream(f, true) //appending to file
  }
  
  private[this] var _outWriter: PrintWriter = null
  
  private[LogError] def outWriter: PrintWriter = {
    this.synchronized {
      if(_outWriter == null)
        _outWriter = new PrintWriter(openCreate(file))
    }
    _outWriter
  }
  
  private[LogError] var errorCount = 0
  
  def errors = errorCount
  
  def close = if(_outWriter != null) _outWriter.close
  
}

case class LogError(e: Throwable){
  import LogError._
  
  def :=(msg: String) = LogError.outWriter.synchronized {
    errorCount += 1
    println("Error: "+msg)
    outWriter.println("<error time='"+DateTime()+"' msg='"+msg+"'>")
    e.printStackTrace(outWriter)
    outWriter.println("</error>")
    outWriter.flush
  }
}