package de.blackpinguin.mediaindexer

import java.net.URL

object HTTP {
  
	def get(url:String) = {
	   val c = new URL(url).openConnection
	   c.setRequestProperty("User-Agent", "evil media indexer")
	   c.getInputStream
	}
	
	def getXML(url:String) =
	  new de.hars.scalaxml.HTMLCleanerFactoryAdapter load url
}