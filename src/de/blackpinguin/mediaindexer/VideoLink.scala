package de.blackpinguin.mediaindexer

case class VideoLink(val title: String, val href: String, val duration: String) {
  private lazy val split = title.split(" ")
  lazy val group = split(0)
  lazy val date = split(1)
  lazy val number = split(2)
  lazy val onlytitle = split.slice(2, split.length).mkString(" ")
  var videolink : String = ""
}

//Begleitobjekt
object VideoLink {

  //regulärer Ausdruck der einen validen Titel beschreibt
  private lazy val p = "\\A\\S+ \\d{4}-\\d{2}-\\d{2} \\S.*\\z".r.pattern

  //ob der Titel entsprechend des regulären Ausdruckes valide ist
  def isValid(title: String) = p.matcher(title).matches

  //Sortierung von VideoLinks
  implicit object vlordering extends Ordering[VideoLink] {
    def compare(a: VideoLink, b: VideoLink) =
      a.title compare b.title
  }

}
