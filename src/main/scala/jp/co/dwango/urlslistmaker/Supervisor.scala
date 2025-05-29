package jp.co.dwango.urlslistmaker

import org.apache.pekko
import pekko.actor.typed.scaladsl.{Behaviors, Routers}
import pekko.actor.typed.{Behavior, SupervisorStrategy}

import okhttp3.*
import java.util.concurrent.TimeUnit

object Supervisor:

  def apply(config: Config): Behavior[SupervisorMessage] =
    Behaviors.setup: ctx =>

      val client =
        OkHttpClient.Builder()
          .connectTimeout(1, TimeUnit.SECONDS)
          .writeTimeout(1, TimeUnit.SECONDS)
          .readTimeout(1, TimeUnit.SECONDS)
          .build()

      val loaderPool =
        Routers
          .pool[WebPageLoaderMessage](config.numOfPageLoader):
            Behaviors
              .supervise(
                WebPageLoader(config, client, ctx.self)
              )
              .onFailure[Exception](SupervisorStrategy.restart)
          .withRoundRobinRouting()

      val router = ctx.spawn(loaderPool, "web-page-loader-pool")

      var fileLoadedUrlCount = 0
      var successCount       = 0
      var failureCount       = 0

      def logAndCheckFinish(): Unit =
        val total = successCount + failureCount
        ctx.log.info(s"total: $total, success: $successCount, failure: $failureCount")
        if total == fileLoadedUrlCount && total > 0 then
          ctx.log.info("All URLs processed — shutting down.")
          ctx.system.terminate()

      Behaviors.receiveMessage:
        case Start =>
          val fileLoader = ctx.spawn(
            UrlsFileLoader(config, ctx.self),
            "urls-file-loader"
          )
          fileLoader ! LoadUrlsFile
          Behaviors.same

        case url @ WebPageUrl(_) =>
          fileLoadedUrlCount += 1
          router ! url
          Behaviors.same

        case DownloadSuccess() =>
          successCount += 1
          logAndCheckFinish()
          Behaviors.same

        case DownloadFailure() =>
          failureCount += 1
          logAndCheckFinish()
          Behaviors.same

        case Finished =>
          Behaviors.same
