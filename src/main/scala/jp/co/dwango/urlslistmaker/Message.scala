package jp.co.dwango.urlslistmaker

sealed trait Message

sealed trait SupervisorMessage extends Message
case object Start extends SupervisorMessage
case object Finished extends SupervisorMessage
case object DownloadSuccess extends SupervisorMessage
case object DownloadFailure extends SupervisorMessage

sealed trait UrlsFileLoaderMessage extends Message
case object LoadUrlsFile extends UrlsFileLoaderMessage

sealed trait WebPageLoaderMessage extends Message

case class WebPageUrl(domain: String)
  extends WebPageLoaderMessage, SupervisorMessage
