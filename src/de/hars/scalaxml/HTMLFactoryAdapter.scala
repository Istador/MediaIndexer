/* © 2009 Florian Hars */
package de.hars.scalaxml

import _root_.scala.xml.parsing.FactoryAdapter

trait HTMLFactoryAdapter extends FactoryAdapter {

  val emptyElements = Set("area", "base", "br", "col", "hr", "img",
    "input", "link", "meta", "param")

  override def nodeContainsText(localName: String) =
    !(emptyElements contains localName)
}
