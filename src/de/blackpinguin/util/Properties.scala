package de.blackpinguin.util

import java.io.{File, FileInputStream}
import java.util.{Properties => Ps}

object Properties {
  
  val default: Ps = new Ps
  val user: Ps = new Ps(default)
  
  def load(f: File) = 
    user.load(new FileInputStream(f))
  
  def addDefault(key: String, value: String): Unit = 
    default.setProperty(key, value)
    
  def addDefault(map: Map[String, String]): Unit = 
    for((key, value) <- map)
      addDefault(key, value)
  
  def get(key: String): Option[String] = {
      val value = Properties(key)
      if(value == null) None
      else Some(value)
    } 
    
  
  def apply(key: String):String = 
    user.getProperty(key)
  
}