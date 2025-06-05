package jp.co.dwango.urlslistmaker

import org.apache.pekko.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.*

object Main:

  @main def runMain(): Unit =
    val urlsFilePath   = "./urls.txt"
    val outputFile     = "./com-sites.txt"
    val numOfPageLoader = 2000

    val config = Config(
      urlsFilePath  = urlsFilePath,
      outputFile    = outputFile,
      numOfPageLoader = numOfPageLoader
    )

    val system: ActorSystem[SupervisorMessage] =
      ActorSystem(Supervisor(config), "urls-list-maker")

    system ! Start

    Await.result(system.whenTerminated, Duration.Inf)
    println("Finished.")