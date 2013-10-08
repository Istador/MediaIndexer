/* © 2009 Florian Hars
 * © 2002-2008 EPFL, Lausanne
 */
package de.hars.scalaxml

import _root_.scala.xml.parsing.FactoryAdapter
import _root_.scala.xml.factory.NodeFactory
import scala.xml.{ Elem, MetaData, NamespaceBinding, Node, Text }

trait NonBindingFactoryAdapter extends FactoryAdapter with NodeFactory[Elem] {

  def nodeContainsText(localName: String) = true

  // methods for NodeFactory[Elem]
  /** constructs an instance of scala.xml.Elem */
  protected def create(pre: String, label: String,
    attrs: MetaData, scpe: NamespaceBinding,
    children: Seq[Node]): Elem =
    Elem(pre, label, attrs, scpe, children: _*)

  // -- methods for FactoryAdapter
  def createNode(pre: String, label: String,
    attrs: MetaData, scpe: NamespaceBinding,
    children: List[Node]): Elem =
    Elem(pre, label, attrs, scpe, children: _*)

  def createText(text: String) = Text(text)

  def createProcInstr(target: String, data: String) =
    makeProcInstr(target, data)
}
