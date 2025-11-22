package jp.co.dwango.urlslistmaker

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.util.ByteString

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.{Paths, StandardOpenOption}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*

object Main:

  private val TitleR = "(?i)<title>(.*?)</title>".r

  // HTMLからタイトルを抽出する関数
  private def extractTitle(html: String): String =
    TitleR.findFirstMatchIn(html).map(_.group(1).trim).getOrElse("")

  // URLからタイトルを取得する関数
  private def fetchTitle(url: String)(using
                                     http: HttpClient,
                                     ec:   ExecutionContext
  ): Future[String] =
    val req = HttpRequest
      .newBuilder(URI.create(s"https://$url"))
      .timeout(java.time.Duration.ofSeconds(10))
      .GET()
      .build()

    http
      .sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .asScala
      .map(_.body())
      .map(extractTitle)
      .recover { case ex => s"ERROR: ${ex.getMessage}" }

  @main def runMain(): Unit =
    val urlsFilePath   = "./urls.txt"
    val outputFile     = "./com-sites.txt"
    val numOfPageLoader = 16

    implicit val system: ActorSystem        = ActorSystem("UrlCrawler")
    implicit val ec:      ExecutionContext  = system.dispatcher
    given      http:      HttpClient        = HttpClient.newHttpClient()

    // urls.txtファイルからURLを読み込むソース
    val urlSource =
      FileIO.fromPath(Paths.get(urlsFilePath))
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
        .map(_.utf8String.trim)
        .filter(_.nonEmpty)

    // URLに対してHTTPリクエストを送信してタイトルを取得するフロー
    val crawlerFlow: Flow[String, (String, String), NotUsed] =
      Flow[String]
        .mapAsyncUnordered(numOfPageLoader)(url => 
          fetchTitle(url).map(title => url -> title)
        )

    // 結果をファイルに書き込むシンク
    val fileSink: Sink[(String, String), Future[IOResult]] =
      Flow[(String, String)]
        .map { case (url, title) => ByteString(s"$url\t$title\n") }
        .toMat(
          FileIO.toPath(
            Paths.get(outputFile),
            Set(
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              StandardOpenOption.TRUNCATE_EXISTING
            )
          )
        )(Keep.right)

    // ストリームを実行
    val done: Future[IOResult] =
      urlSource.via(crawlerFlow).runWith(fileSink)

    done.onComplete: r =>
      println(s"stream finished: $r")
      system.terminate()