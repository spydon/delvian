package modules

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage}

class Calc(override val bus: MessageEventBus) extends AbstractBot {

  override def help(channel: String): OutboundMessage =
    OutboundMessage(channel,
      "`$calc {operation} {arguments separated by space}`")

  val possibleOperations = Map(
    "+" -> ((x: Double, y: Double) => x+y),
    "-" -> ((x: Double, y: Double) => x-y),
    "*" -> ((x: Double, y: Double) => x*y),
    "/" -> ((x: Double, y: Double) => x/y)
  )

  override def act: Receive = {
    case Command("calc", operation :: args, message) if args.length >= 1 =>
      val op = possibleOperations.get(operation)

      val response = op.map(f => {
        val result = args.map(_.toDouble).reduceLeft( f(_,_) )
        OutboundMessage(message.channel, s"Result is: $result")
      }).getOrElse(OutboundMessage(message.channel, s"No operation $operation"))

      publish(response)

    case Command("calc", _, message) =>
      publish(OutboundMessage(message.channel, s"No arguments specified!"))
  }
}
