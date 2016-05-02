package modules

import io.scalac.slack.{common, MessageEventBus}
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{DirectMessage, Command, OutboundMessage, UserInfo}
import java.io._
import org.joda.time.DateTime

import scala.io.Source
import scala.language.postfixOps

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

class Holiday(override val bus: MessageEventBus) extends AbstractBot {
  val filename = "holiday.txt" // TODO: Should use some calender API instead

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    "`$holiday` tells you the next day off\\n" +
    "`$holiday @user` tells the user which the next day off is"

  def readNextHoliday(): String = {
    println(DateTime.now)
    try {
      for (line <- Source.fromFile(filename).getLines()) {
        val kv = line.split(" :: ")
        val date = DateTime.parse(kv(0))
        val text = kv(1)
        val daysLeft = Math ceil (date.getMillis-DateTime.now.getMillis)/1000.0/86400.0
        if(!date.isBeforeNow) return daysLeft.toInt + " days left until " + text
      }

      "No more holidays found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  def listHolidays(): String = {
    var holidays = ""
    try {
      for (line <- Source.fromFile(filename).getLines()) {
        holidays += line + "\\n"
      }

      if(holidays != "") holidays.stripSuffix("\\n")
      else "No holidays found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    case Command("holiday", List("list"), message) =>
      publish(OutboundMessage(message.channel, listHolidays()))

    case Command("holiday", keys, message) =>
      val builtMessage = if(keys.nonEmpty && keys.last.contains("@U")) keys.last + ": " + readNextHoliday() else readNextHoliday()
      val response = OutboundMessage(message.channel, builtMessage)

      publish(response)
  }

}
