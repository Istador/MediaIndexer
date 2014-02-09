package de.blackpinguin.mediaindexer

import de.blackpinguin.mediaindexer.{ VideoLink => VL }
import collection.immutable.{ SortedSet => Set, SortedMap => Map }

object Output {

  //abkürzung für die Map-Struktur
  type SubTree = Map[String, Set[VL]]
  def SubTree() = Map[String, Set[VL]]()
  type Tree = Map[String, SubTree]
  def Tree() = Map[String, SubTree]()

  private lazy val umlaute = Map[String, String](
    "&Atilde;&Yuml;" -> "ß", //sz
    "&Atilde;&frac14;" -> "ü", //uuml
    "&Atilde;&curren;" -> "ä", //auml
    "&Atilde;&para;" -> "ö", //ouml
    "&Atilde;&oelig;" -> "Ü", //Uuml
    "&Atilde;&bdquo;" -> "Ä", //Auml
    "&szlig;" -> "ß",
    "&uuml;" -> "ü",
    "&auml;" -> "ä",
    "&ouml;" -> "ö",
    "&Uuml;" -> "Ü",
    "&Auml;" -> "Ä",
    "&Ouml;" -> "Ö")

  //Umlaute im Titel ersetzen (sonst wird das & bei der Ausgabe zu &amp;)
  def fixUmlaute(in: String) = {
    var out = in
    for (fix <- umlaute)
      out = out.replace(fix._1, fix._2)
    out
  }

  //erzeugt aus dem Set von VideoLinks eine geschachtelte Map-Struktur
  def toTree(set: Set[VL]): Tree = {
    var m = Tree()
    for (e <- set) 
      m = insertInTree(e, m)
    m
  }

  //fügt ein einzelnes Element in die Baumstruktur ein
  private def insertInTree(e: VL, old: Tree): Tree = {
    var m = old
    //Gruppe existiert noch nicht
    if (!m.contains(e.group))
      m += (e.group -> SubTree())
    //Datum existiert noch nicht
    if (!m(e.group).contains(e.date)) 
      m += (e.group -> m(e.group).+(e.date -> Set[VL]()))
    //Element einfügen
    m + ((e.group -> m(e.group).+(e.date -> (m(e.group)(e.date) + e))))
  }

  //erzeugt das HTML-Dokument aus einem Set aus Videos 
  def toHtml(set: Set[VL]) = {
    //aus dem Set eine Baumstruktur machen
    val tree = toTree(set)
    
    //HTML-erzeugen
    <html lang="de">
      <head>
        <title>Video Links</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <script type="text/javascript" src="jquery-1.10.2.min.js"></script>      
    	<script type="text/javascript" src="script.js"></script>
    	<link rel="stylesheet" type="text/css" href="style.css" />
      </head>
      <body>
        {
          for (g <- tree.keySet.toArray) yield {
            <details id={ "d_" + g }><summary>{ g }</summary><div>{
              for (d <- tree(g).keySet.toArray) yield {
                <details id={ "d_" + g + "_" + d }><summary><input type="checkbox" name={ "cb_" + g + " " + d }/>{ d }</summary><div>{
                  (for (e <- tree(g)(d).toArray) yield {
                    <div>
                      <input type="checkbox" name={ e.title }/>
                      <a href={ e.url } target="_blank">{ e.onlytitle }</a>
                      <span>({e.duration})</span>
                      <a href={ e.videolink + ".flv" } target="_blank">[flv]</a>
                      <a href={ e.videolink + ".m4v" } target="_blank">[mp4]</a>
                      <a href={ e.videolink + ".webm" } target="_blank">[webm]</a>
                    </div>
                  })
                }</div></details>
              }
            }</div></details>
          }
        }<br/><br/>
    	<input type="button" id="check" value="check all"/>
    	<input type="button" id="uncheck" value="uncheck all"/>
    	<p>Diese Seite wurde erstellt von <a href="https://blackpinguin.de/" target="_blank">Robin C. Ladiges</a>.</p>
      </body>
    </html>
  }

}