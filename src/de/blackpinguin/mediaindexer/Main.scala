package de.blackpinguin.mediaindexer

import java.net.{ URL, URLConnection }
import collection.immutable.{ SortedSet => Set }

import scala.concurrent.{Promise, Future}

object Main {
  
  import de.blackpinguin.util._
  
  def main(args: Array[String]): Unit = {
    de.blackpinguin.util.Properties.load(new java.io.File("user.conf"))
    
    
    if(args.length == 0){
      xslt
    }
    else if(args.length == 1){
      args(0) match{
        case "help" => printUsage
        case "-help" => printUsage
        case "--help" => printUsage
        case "h" => printUsage
        case "-h" => printUsage
        case "--h" => printUsage
        
        case "-small" => small
        case "-full" => full
        case "-xslt" => xslt
        
        case _ => printUsage
      }
    } else if(args.length == 2){
      args(0) match{
        case "-update" => update(args(1))
        case "-remove" => remove(args(1))
        case _ => printUsage
      }
    } else 
      printUsage
  }

  def printUsage = {
    println("MediaIndexer\t\t\tsmal run")
    println("MediaIndexer -small\t\tsmall run")
    println("MediaIndexer -full\t\tfull run")
    println("MediaIndexer -xslt\t\ttransform videos.xml using the XSLT files")
    println("MediaIndexer -update URL\tupdate a single video")
    println("MediaIndexer -remove URL\tremove a single video")
  }
  
  
  def small { Layer.init ; Mediathek.small ; xslt }
  
  def full { Mediathek.full ; xslt }
  
  def xslt { de.blackpinguin.util.Time.measureAndPrint{XSLT()} }
  
  def update(url:String){ Mediathek.update(url) }
  def remove(url:String){ XML.remove(url) }
  
  
}