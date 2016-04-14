package modules

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.system.HelpBot
import io.scalac.slack.common.OutboundMessage

class Help(override val bus: MessageEventBus) extends HelpBot(bus) {

  override def help(channel: String): OutboundMessage =
    OutboundMessage(channel,
      "`$help {module}` shows help for a specific module\\n" +
      "`$help` shows help for all modules")

}
