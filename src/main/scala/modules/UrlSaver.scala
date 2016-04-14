package modules

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{DirectMessage, Command, OutboundMessage}
import java.io._
import scala.io.Source
import scala.language.postfixOps

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps



class UrlSaver(override val bus: MessageEventBus) extends AbstractBot {
  val filename = "urls.txt"

  import context._

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    "`$url add key(s) url` saves the url with the key(s) as lookup keys\\n" +
    "`$url key` gets the urls associated with the key"

  def writeUrl(keys: List[String], url: String): String = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file, true))
    val prettyKeys = keys.foldLeft("")((acc, key) => acc + " " + key).stripPrefix(" ")
    val prettyUrl = url.split("\\|")(0).stripPrefix("<").stripSuffix(">")
    bw.write(prettyKeys + " :: " + prettyUrl + "\n")
    bw.close()
    prettyUrl
  }

  def readUrl(key: String): String = {
    var urls = ""
    try {
      for (line <- Source.fromFile(filename).getLines()) {
        val kv = line.split(" :: ")
        val keys = kv(0).split(" ")
        val url = kv(1)
        if(keys.contains(key)) {
          urls += " " + url
        }
      }
      if(urls != "") urls.stripPrefix(" ").replace(" ", "\\n")
      else "No such url found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  def listUrls(): String = {
    var urls = ""
    try {
      for (line <- Source.fromFile(filename).getLines()) {
        val kv = line.split(" :: ")
        urls += kv(1) + " - " + kv.head + "\\n"
      }

      if(urls != "") urls.stripSuffix("\\n")
      else "No urls found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    // Checks whether the message contains any trigger word
    case Command("url", "add" :: url :: keys, message) =>
      println(message)
      val response = OutboundMessage(message.channel, writeUrl(keys, url))

      publish(response)

    case Command("url", List("list"), message) =>
      println(message + listUrls())
      publish(DirectMessage(message.user, listUrls()))

    case Command("url", keys, message) =>
      val key = if(keys.head == "get") keys(1) else keys.head
      val response = OutboundMessage(message.channel, readUrl(key))

      publish(response)
  }

}
