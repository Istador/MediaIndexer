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
  
  private[LogError] lazy val outWriter = new PrintWriter(openCreate(file))
  
  private[LogError] var errorCount = 0
  
  def errors = errorCount
  
  def close = outWriter.close
  
}

case class LogError(e: Throwable){
  def :=(msg: String) = LogError.outWriter.synchronized {
    LogError.errorCount += 1
    LogError.outWriter.println("<error time='"+DateTime()+"' msg='"+msg+"'>")
    e.printStackTrace(LogError.outWriter)
    LogError.outWriter.println("</error>")
    LogError.outWriter.flush
  }
}