package ch.wsl.box.services.mail_dispatcher

trait MailDispatcherService {
  def start()
  def dispatchNow()
}
