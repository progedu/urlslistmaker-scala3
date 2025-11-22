package jp.co.dwango.urlslistmaker

import org.apache.pekko.actor.typed.ActorRef

sealed trait Message

sealed trait SupervisorMessage extends Message
case object Start extends SupervisorMessage
case object Finished extends SupervisorMessage, WebPageLoaderMessage
case object DownloadSuccess extends SupervisorMessage
case object DownloadFailure extends SupervisorMessage

sealed trait UrlsFileLoaderMessage extends Message
case class LoadUrlsFile(replyTo: ActorRef[WebPageLoaderMessage]) extends UrlsFileLoaderMessage

sealed trait WebPageLoaderMessage extends Message
case object LoadWebPage extends WebPageLoaderMessage

case class WebPageUrl(domain: String)
  extends WebPageLoaderMessage
