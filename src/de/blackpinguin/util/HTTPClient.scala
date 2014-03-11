package de.blackpinguin.util

import scala.concurrent.{ Future, Promise, Await }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.Executor
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, Response }
import org.w3c.dom.Document
import java.io.InputStream
import org.htmlcleaner.{ DomSerializer => Serializer }
import org.htmlcleaner.{ HtmlCleaner => Cleaner }
import org.w3c.dom.Document



//Um auch HTML das nicht well-formed XML ist zu parsen 
trait HTMLCleaner {

  import org.htmlcleaner.{ HtmlCleaner => Cleaner, DomSerializer => Serializer }

  private[this] val cleaner = ThreadSafe(new Cleaner)
  private[this] val props = cleaner.get.getProperties()
  props.setNamespacesAware(true) //um sich nicht beim parsen an namespaces aufzuhängen
  props.setCharset("UTF-8")
  props.setTranslateSpecialEntities(true)
  props.setAdvancedXmlEscape(true)
  props.setRecognizeUnicodeChars(true)
  private[this] val serializer = ThreadSafe(new Serializer(props, true))

  def clean(is: InputStream): Document =
    serializer.get.createDOM(cleaner.get.clean(is, "UTF-8"))
}



object HTTP {
  //Exception wenn HTTP-Anfrage nicht erfolgreich
  case class StatusException(status: Int) extends RuntimeException
}



// "Interface" mit Funktionalität
trait HTTP extends HTMLCleaner {
  
  //Response Objekt
  protected def get(url: String)(implicit exec: Executor): Future[Response]

  def getResponse(url: String): Future[Response] = get(url)
  
  //Input Stream
  def getStream(url: String): Future[InputStream] =
    for (response <- get(url))
      yield response.getResponseBodyAsStream

  //String
  def getString(url: String): Future[String] =
    for (response <- get(url))
      yield response.getResponseBody

  //Document
  def getDOM(url: String): Future[Document] =
    for (is <- getStream(url))
      yield clean(is)
  
}



//Asynchroner HTTP Client (nicht blockierend)
object AsyncHTTP extends HTTP {

  private[this] val builder = ThreadSafe{
    val tmp = new AsyncHttpClientConfig.Builder()
    tmp.setFollowRedirects(true)
    tmp
  }
  
  private[this] val client = ThreadSafe(new AsyncHttpClient(builder.get.build))
  

  protected def get(url: String)(implicit exec: Executor): Future[Response] = {
    val f = client.get.prepareGet(url).setHeader("Accept-Charset", "utf-8").execute()
    val p = Promise[Response]()
    f.addListener(new Runnable {
      def run = {
        val response = f.get
        if (response.getStatusCode / 100 < 4)
          p.success(response)
        else
          p.failure(HTTP.StatusException(response.getStatusCode))
      }
    }, exec)
    p.future
  }

}



//Synchroner HTTP Client (blockierend)
object SyncHTTP extends HTTP {

  //notfalls unendlich lange warten
  var maxWaitingTime: Duration = Duration.Inf

  def get(url: String)(implicit exec: Executor): Future[Response] = {
    val f = AsyncHTTP.getResponse(url)
    Await.ready(f, maxWaitingTime)
    f
  }
}



object WebClientTest extends App {

  def client: HTTP = AsyncHTTP // ~ 2.7 sec for 500 requests
  //def client: HTTP = SyncHTTP // ~ 3.13 min for 500 requests
  
  var r = 0
  def inc(n: Int) = this.synchronized { r = r + 1; println(r + ": " + n) }
  
  
  Time.measureAndPrint {
    val fs = for (i <- 1 to 500) yield {
      val n = i

      val f = client.getDOM("http://www.google.de/search?q=" + i)
      f.onComplete {
        case scala.util.Success(data) =>
          inc(n)
        case scala.util.Failure(exc) =>
          println("Error " + n + ": " + exc.getMessage)
      }
      f
    }

    fs.foreach { f => Await.ready(f, Duration.Inf) }
  }
  
}


