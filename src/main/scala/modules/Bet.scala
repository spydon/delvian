package modules

import java.io._

import akka.util.Timeout
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, DirectMessage, OutboundMessage}

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.Try

class Bet(override val bus: MessageEventBus) extends AbstractBot {
  val filename = "bets.txt"

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    "`$bet guess` saves your PSU guess\\n" +
    "`$bet list` gets the placed bets"

  def writeBet(guess: String, user: String): String = {
    try {
      val guessNumber = guess.toLong
      val hasPlacedBet = Source.fromFile(filename).getLines().mkString(" ").contains(user)
      if(hasPlacedBet) throw new Exception("You have already placed a bet")
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file, true))
      val line = s"$guessNumber :: <@$user>\n"
      bw.write(line)
      bw.close()
      line.stripSuffix("\n")
    } catch {
      case ex: NumberFormatException => "Not a number"
      case general: Exception => general.getMessage
    }
  }

  def listBets(): String = {
    try {
      val bets = Source.fromFile(filename).getLines().map { line =>
        val kv = line.split(" :: ")
        (kv(0).toLong, kv(1))
      }.toList
        .sortBy{ case (guess, nick) => guess }
        .map{ case (guess, user) => s"$guess :: $user" }
        .mkString("\\n")


      if(bets != "") bets.stripSuffix("\n")
      else "No bets found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    // Checks whether the message contains any trigger word
     case Command("bet", List("list"), message) =>
      val response = OutboundMessage(message.channel, s">>> ${listBets()}")
      publish(response)

     case Command("bet", bet, message) =>
      val response = OutboundMessage(message.channel, writeBet(bet.head, message.user))
      publish(response)
  }

}
