package modules

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage}
import org.joda.time.DateTime

class Friday(override val bus: MessageEventBus) extends AbstractBot {
  val r = scala.util.Random
  val fridayMessages =
    List("IT IS!!!", "Trevlig helg", "modules.Friday, it is!", "Have a great weekend", "See you on monday",
    ":beers:", ":rebeccablack:")

  val sadMessages =
    List("Trevlig vardag", "Unfortunately not", "Doesn't matter...", "Still a bit to go", "NEIN", "Check the calendar",
         "You work in a great place, stop asking!", "How come your existence revolve around a day that is not today?",
         "Nope.", ":'(", ":lipssealed:")

  val triggerWords = List("friday", "fredag", "vrijdag", "reede", "perjantai", "freitag", "föstudagur", "jumat",
                          "venerdì", "piątek", "vineri", "viernes")

  override def help(channel: String): OutboundMessage =
    OutboundMessage(channel,
      s"$name will help you to know whether it is friday or not \\n" +
      "Usage: call on the bot and include friday in the message")

  def trigger(msg: String): Boolean = {
    val stripped = msg.replace("?", "").replace("!", "").replace(".", "")
    triggerWords.contains(stripped.toLowerCase)
  }

  def isFriday(): Boolean = {
    DateTime.now.dayOfWeek.get == 5
  }

  def resultMessage(): String = {
    if(isFriday()) fridayMessages(r.nextInt(fridayMessages.length))
    else           sadMessages(r.nextInt(sadMessages.length))
  }

  override def act: Receive = {
    case Command(command, args, message) if (command :: args).foldLeft(false)((acc, c) => acc || trigger(c)) =>
      val response = OutboundMessage(message.channel, s"$resultMessage")

      publish(response)
  }
}
