package jp.co.dwango.urlslistmaker

import org.apache.pekko
import pekko.actor.typed.{ActorRef, Behavior}
import pekko.actor.typed.scaladsl.Behaviors

import okhttp3.*

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.util.{Failure, Success, Try}

object WebPageLoader:

  def apply(
             config: Config,
             client: OkHttpClient,
             supervisor: ActorRef[SupervisorMessage]
           ): Behavior[WebPageLoaderMessage] =

    Behaviors.setup: context =>

      val targetPath = Paths.get(config.outputFile)
      if Files.notExists(targetPath) then Files.createFile(targetPath)

      Behaviors.receiveMessage:

        case WebPageUrl(domain) =>
          val url      = s"https://$domain.com"
          val request  = Request.Builder().url(url).build()

          client.newCall(request).enqueue(new Callback:
            override def onFailure(call: Call, e: IOException): Unit =
              supervisor ! DownloadFailure()          // 通信自体が失敗

            override def onResponse(call: Call, response: Response): Unit =
              try
                if response.isSuccessful then
                  val body       = response.body.string
                  val titleRegex = "<title>(.+?)</title>".r
                  val title      = titleRegex.findFirstMatchIn(body).map(_.group(1)).getOrElse("None")
                  val line       = s"$domain.com\t$title\n"

                  Try:
                    Files.write(
                      targetPath,
                      line.getBytes(StandardCharsets.UTF_8),
                      StandardOpenOption.APPEND
                    )
                  match
                    case Success(_) => supervisor ! DownloadSuccess()
                    case Failure(_) => supervisor ! DownloadFailure()  // 書き込み失敗
                else
                  supervisor ! DownloadFailure()      // HTTP ステータスが失敗
              finally
                response.close()
          )

          Behaviors.same
