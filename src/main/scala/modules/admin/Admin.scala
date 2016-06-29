package modules.admin

import akka.util.Timeout
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage}

import scala.concurrent.duration._
import scala.language.postfixOps

class Admin(override val bus: MessageEventBus) extends AbstractBot {

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    "`$admin` handles the users that have access to higher privilege\\n" +
    "`$admin @user` gives the user privilege"

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    case Command("admin", List("list"), message) =>
      val response = OutboundMessage(message.channel, AdminUtil.listAdmins())
      publish(response)

    case Command("admin", "delete" :: users, message) =>
      users.foreach(user => {
        val response = OutboundMessage(message.channel, AdminUtil.deleteUser(user, message.user))
        publish(response)
      })

    case Command("admin", List(user), message) =>
      val response = OutboundMessage(message.channel, AdminUtil.addUser(user, message.user))
      publish(response)
  }

}
