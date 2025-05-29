package jp.co.dwango.urlslistmaker

import org.apache.pekko
import pekko.actor.typed.scaladsl.{Behaviors, Routers}
import pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}

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

      val fileLoader = ctx.spawn(
        UrlsFileLoader(config),
        "urls-file-loader"
      )

      val loaderPool =
        Routers
          .pool[WebPageLoaderMessage](config.numOfPageLoader):
            Behaviors
              .supervise(
                WebPageLoader(config, client, ctx.self, fileLoader)
              )
              .onFailure[Exception](SupervisorStrategy.restart)
          .withRoundRobinRouting()

      val router = ctx.spawn(loaderPool, "web-page-loader-pool")

      var finishCount        = 0
      var successCount       = 0
      var failureCount       = 0

      def logProgress(): Unit =
        val total = successCount + failureCount
        ctx.log.info(s"total: $total, success: $successCount, failure: $failureCount")

      Behaviors.receiveMessage:
        case Start =>
          for _ <- 1 to config.numOfPageLoader do
            router ! LoadWebPage
          Behaviors.same

        case DownloadSuccess() =>
          successCount += 1
          logProgress()
          Behaviors.same

        case DownloadFailure() =>
          failureCount += 1
          logProgress()
          Behaviors.same

        case Finished =>
          finishCount += 1
          if finishCount == config.numOfPageLoader then
            ctx.log.info("All WebPageLoaders finished — shutting down.")
            ctx.system.terminate()
          Behaviors.same
