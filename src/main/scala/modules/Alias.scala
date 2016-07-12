package modules

import java.io._

import akka.util.Timeout
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{BaseMessage, Command, OutboundMessage}

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

class Alias(override val bus: MessageEventBus) extends AbstractBot {
  val filename = "alias.txt"
  var lastTs = "" //TODO: Fix ugly hack to remove duplicate answers

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    ">>>`$alias` or `$a` creates an alias\\n" +
    "`$alias add name command` saves an alias"

  def writeAlias(alias: String, commands: List[String]): String = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file, true))
    val command = commands.mkString(" ")
    bw.write(alias + " :: " + command + "\n")
    bw.close()
    "`" + alias + "` added"
  }

  def readAlias(key: String): Option[Array[String]] = {
    try {
      var commands: Option[Array[String]] = None
      for (line <- Source.fromFile(filename).getLines()) {
        val kv = line.split(" :: ")
        val keys = kv(0).split(" ")
        if(keys(0) == key) {
          commands = Some(kv(1).split(" "))
        }
      }

      return commands
    } catch {
      case ex: FileNotFoundException => None
      case ex: IOException => None
    }
  }

  def listAlias(): String = {
    try {
      ">>>" + Source.fromFile(filename).getLines().mkString("\\n")

    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    // Checks whether the message contains any trigger word
    case Command("alias", "add" :: alias :: command, message) =>
      val response = OutboundMessage(message.channel, writeAlias(alias, command))
      publish(response)

    case Command("alias", List("list"), message) => publish(OutboundMessage(message.channel, listAlias()))
    case Command("a", keys, message) => act(Command("alias", keys, message))
    case Command(alias, List(), message) if message.ts != lastTs =>
      lastTs = message.ts //TODO: Fix ugly hack to remove duplicate answers
      readAlias(alias) match {
        case Some(command) =>
          //val msg = Command(command.head, command.tail.toList, message)
          // BaseMessage instead of command so that it goes through the CommandRecognizer
          val msg = BaseMessage("$" + command.mkString(" "), message.channel, message.user, message.ts, message.edited)
          publish(msg)
        case None =>
          println("GOES TO NONE")
          // Do not publish anything as other commands might be one word
      }
  }
}
