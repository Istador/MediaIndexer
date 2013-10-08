/* © 2009 Florian Hars */
package de.hars.scalaxml

import _root_.java.io.Reader
import _root_.org.htmlcleaner.{ HtmlCleaner, DomSerializer }

class HTMLCleanerFactoryAdapter extends DOMFactoryAdapter with HTMLFactoryAdapter {

  private val cleaner = new HtmlCleaner
  private val props = cleaner.getProperties()
  props.setNamespacesAware(true)
  private val serializer = new DomSerializer(props, true)

  def getDOM(reader: Reader) = {
    val node = cleaner.clean(reader)
    serializer.createDOM(node);
  }
}
