package modules

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage}
import org.joda.time.DateTime

class Friday(override val bus: MessageEventBus) extends AbstractBot {
  var lastTs = "" //TODO: Fix ugly hack to remove duplicate answers
  val fridayMessages =
    List("IT IS!!!", "Trevlig helg :partyparrot:", "Friday, it is! :yoda:", "Have a great weekend",
         "See you on monday!", ":beers:", ":rebeccablack:", ":partyparrot:", ":party:",
         "Yup! What are you going to do with all that free time?!", "Damn right it is! :smile:", "JAA, DAS IST KLAR!",
         "Do you want it to be friday? In that case I guess it is.")

  val sadMessages =
    List("Trevlig vardag", "Unfortunately not", "Doesn't matter...", "Still a bit to go", "NEIN", "Check the calendar",
         "You work in a great place, stop asking!", "How come your existence revolve around a day that is not today?",
         "Nope.", ":sadpanda:", ":lipssealed:")

  val triggerWords =
    List("friday", "fredag", "vrijdag", "reede", "perjantai", "freitag", "föstudagur", "jumat", "venerdì", "piątek",
         "vineri", "viernes", "الجمعة", "petak")

 def trigger(msg: String): Boolean = {
    val stripped = msg.replace("?", "").replace("!", "").replace(".", "").replace(",", "")
    triggerWords.contains(stripped.toLowerCase)
  }

  def isFriday(): Boolean = {
    DateTime.now.dayOfWeek.get == 5
  }

  def resultMessage(): String = {
    val r = scala.util.Random
    if(isFriday()) fridayMessages(r.nextInt(fridayMessages.length))
    else           sadMessages(r.nextInt(sadMessages.length))
  }

  // If a user tries to trick the bot with not
  def notMessage(notNumber: Int): String = {
    var not = ""
    for(_ <- 1 to notNumber) not = not.concat("not ")

    if(notNumber % 2 == 0) resultMessage()
    else if(isFriday) "It is " + not + "not friday! " + resultMessage()
    else "It is " + not + "friday... " + resultMessage()
  }

  override def help(channel: String): OutboundMessage =
    OutboundMessage(channel, "Say `Friday` in your mother tongue and it will trigger a friday check")

  override def act: Receive = {
    // Checks whether the message contains any trigger word
    case Command(command, args, message) if (command :: args).foldLeft(false)((acc, c) => acc || trigger(c)) && message.ts != lastTs =>
      println(DateTime.now)
      lastTs = message.ts //TODO: Fix ugly hack to remove duplicate answers
      val notNumber = (command :: args).foldLeft(0)((acc, c) => acc + (if(c.toLowerCase == "not") 1 else 0))
      val result = if(notNumber > 0) notMessage(notNumber) else resultMessage()
      val response = OutboundMessage(message.channel, s"$result")

      publish(response)
  }

}
