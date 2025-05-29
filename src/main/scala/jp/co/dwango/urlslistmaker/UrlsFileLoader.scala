package jp.co.dwango.urlslistmaker

import org.apache.pekko
import pekko.actor.typed.{ActorRef, Behavior}
import pekko.actor.typed.scaladsl.Behaviors

import scala.io.{Codec, Source}

object UrlsFileLoader:

  def apply(config: Config): Behavior[UrlsFileLoaderMessage] =
    Behaviors.setup: context =>
      
      val file = Source.fromFile(config.urlsFilePath)(Codec.UTF8)
      val urlsIterator = file.getLines()

      Behaviors.receiveMessage:

        case LoadUrlsFile(replyTo) =>
          if urlsIterator.hasNext then
            val line = urlsIterator.next()
            replyTo ! WebPageUrl(line)
          else
            replyTo ! Finished
            file.close()
          Behaviors.same
