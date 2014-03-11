package de.blackpinguin.util

import java.util.{ Date => D, Calendar => C, GregorianCalendar => GC, TimeZone => TZ, Locale => L }
import java.text.SimpleDateFormat
import scala.util.DynamicVariable


object TestDates extends App {
  import Dates._
  
  println(Date())
  println(Date("2013-02-03").day)
  println(DateTime())
}



object Dates {
  
  
  
  trait DateTrait {
    
    def date: D
    
    val cal = new GC(TZ.getTimeZone("Europe/Berlin"), L.GERMANY)
    cal.setTime(date)
    
    def day = cal.get(C.DAY_OF_MONTH)
    def month = cal.get(C.MONTH)+1
    def year = cal.get(C.YEAR)
  }
  
  
  
  trait DateTimeTrait extends DateTrait {
    def hour = cal.get(C.HOUR_OF_DAY)
    def minute = cal.get(C.MINUTE)
    def second = cal.get(C.SECOND)
  }
  
  
  
  object Date {
    //DynamicVariable: Jeder Thread-Pool bekommt seine eigene Kopie
    private[Date] val sdf = new ThreadLocal[SimpleDateFormat](){
      override def initialValue():SimpleDateFormat =  
        new SimpleDateFormat("yyyy-MM-dd")
    }
    //private[this] val sdf = new DynamicVariable(new SimpleDateFormat("yyyy-MM-dd"))
    
    private[Date] val sdf2 = new ThreadLocal[SimpleDateFormat](){
      override def initialValue():SimpleDateFormat =  
        new SimpleDateFormat("dd.MM.yyyy")
    }
    //private[this] val sdf2 = new DynamicVariable(new SimpleDateFormat("dd.MM.yyyy"))
    
    //Matcht Datum in einem beliebigen String
    private[this] val dateRE = new DynamicVariable("""\A.*(\d{4}-\d{2}-\d{2}).*\z""".r)
    private[this] val dateRE2 = new DynamicVariable("""\A.*(\d{2}\.\d{2}\.\d{4}).*\z""".r)
    
    def apply(str: String):Date = {
      try{
        dateRE.value.findFirstMatchIn(str) match {
          case Some(m) => Date(sdf.get.parse(m.group(1)))
          case None => dateRE2.value.findFirstMatchIn(str) match {
            case Some(m) => Date(sdf2.get.parse(m.group(1)))
            case None => null
          }
        } 
      } catch {
        case t:Throwable =>
          println("Date Error: "+str)
          t.printStackTrace
          throw t
      }
    }
    
  }
  
  
  
  case class Date(date: D = new D()) extends DateTrait {
    override def toString: String = Date.sdf.get.format(date)
  }
  
  
  import scala.language.implicitConversions
  implicit def String2Date(str: String): Date = Date(str)
  implicit def Date2String(d: Date):String = d.toString
  implicit def Date2DateTime(dt: Date):DateTime = DateTime(dt.date)
  
  
  
  object DateTime {
    
    private[DateTime] val sdf = new DynamicVariable(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
    
    //Matcht Datum in einem beliebigen String
    private[this] val dateRE = new DynamicVariable("""\A.*(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).*\z""".r)
    
    def apply(str: String):DateTime = {
        dateRE.value.findFirstMatchIn(str) match {
          case Some(m) => DateTime(sdf.value.parse(m.group(1)))
          case None => null
      }
    }
    
  }
  
  
  
  case class DateTime(date: D = new D()) extends DateTimeTrait {
    override def toString: String = DateTime.sdf.value.format(date)
  }
  
  
  
  implicit def String2DateTime(str: String): DateTime = DateTime(str)
  implicit def DateTime2String(dt: DateTime):String = dt.toString
  implicit def DateTime2Date(dt: DateTime):Date = Date(dt.date)
  
  
  
}