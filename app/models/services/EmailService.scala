package models.services


object EmailService {

  import mail._
  def sendEmail(from: String, fromEmail: String, to:Seq[String], cc: Seq[String], subject: String, message: String) = {
    send a new Mail(
      from = (fromEmail, from),
      to = to,
      cc = cc,
      subject = subject,
      message = message
    )
  }
}