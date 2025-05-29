package jp.co.dwango.urlslistmaker

trait Message

sealed trait SupervisorMessage extends Message
case object Start extends SupervisorMessage
case object Finished extends SupervisorMessage
case class DownloadSuccess() extends SupervisorMessage
case class DownloadFailure() extends SupervisorMessage

sealed trait UrlsFileLoaderMessage extends Message
case object LoadUrlsFile extends UrlsFileLoaderMessage

sealed trait WebPageLoaderMessage extends Message

case class WebPageUrl(domain: String)
  extends WebPageLoaderMessage with SupervisorMessage
