package de.blackpinguin.mediaindexer

import de.blackpinguin.mediaindexer.{ VideoLink => VL }
import scala.collection.mutable.Set
import collection.immutable.{SortedSet => SSet}
import java.io.{BufferedWriter, FileWriter, BufferedReader, FileReader}

object Saver {
  
  // VideoLinks in Datei speichern
  def save(data: Seq[VL]) : Unit = {
    val bw = new BufferedWriter(new FileWriter("videolinks.db"))
    for (vl <- data) {
      bw.write(vl.title)
      bw.write("\n")
      bw.write(vl.href)
      bw.write("\n")
      bw.write(vl.duration)
      bw.write("\n")
      bw.write(vl.videolink)
      bw.write("\n")
    }
    bw.close
  }

  //lädt VideoLinks aus der Datei
  def load : SSet[VL] = {
    val set = Set[VL]()

    try {

      val br = new BufferedReader(new FileReader("videolinks.db"))

      var ok = true
      while (ok) {
        val title = br.readLine
        ok = title != null
        if (ok) {
          val href = br.readLine
          val duration = br.readLine
          val vl = VL(title, href, duration)
          vl.videolink = br.readLine
          set += vl
        }
      }

      br.close
    } catch { case _: Throwable => }
    
    SSet[VL]() ++ set
  }

}