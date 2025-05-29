package jp.co.dwango.urlslistmaker

import org.apache.pekko
import pekko.actor.typed.{ActorRef, Behavior}
import pekko.actor.typed.scaladsl.Behaviors

import scala.io.{Codec, Source}

object UrlsFileLoader:

  def apply(config: Config, supervisor: ActorRef[SupervisorMessage]): Behavior[UrlsFileLoaderMessage] =
    Behaviors.receiveMessage:

      case LoadUrlsFile =>
        val file = Source.fromFile(config.urlsFilePath)(Codec.UTF8)
        try
          for line <- file.getLines() do
            supervisor ! WebPageUrl(line)
        finally
          file.close()

        Behaviors.same
