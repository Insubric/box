package ch.wsl.box.services.mail_dispatcher

class DummyMailDispatcherService extends MailDispatcherService {
  override def start(): Unit = {
    println("Start mail dispatching")
  }

  override def dispatchNow(): Unit = {
    println("mail dispatchNow")
  }
}
