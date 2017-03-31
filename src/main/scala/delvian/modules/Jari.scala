package delvian.modules

import java.io._

import akka.util.Timeout
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, DirectMessage, OutboundMessage}

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.Random

class Jari(override val bus: MessageEventBus) extends AbstractBot {
  val filename = "jari.txt"

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    "`$jari add stupid joke` saves the stupid joke\\n" +
    "`$jari [@user]` tells a stupid jari joke"

  def writeJoke(joke: String): String = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file, true))
    bw.write(s"${joke.replace("\n", "\\n").replace("\"","\\\"")}\n")
    bw.close()
    "Stupid joke saved... :expressionless:"
  }

  def readJoke(): String = {
    try {
      val jokes = Source.fromFile(filename).getLines().toList
      Random.shuffle(jokes).head
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    // Checks whether the message contains any trigger word
    case Command("jari", "add" :: joke, message) =>
      println(message)
      val response = OutboundMessage(message.channel, writeJoke(joke.mkString(" ")))

      publish(response)

    case Command("jari", keys, message) =>
      val builtMessage = {
        if(keys.length > 0 && keys.last.contains("@U"))
          keys.last + ": " + readJoke()
        else
          readJoke()
      }
      val response = OutboundMessage(message.channel, builtMessage)

      publish(response)
  }

}
