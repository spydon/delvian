package modules

import akka.util.Timeout
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage, IncomingMessage}
import modules.admin.AdminUtil

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success}
import sys.process._

import org.apache.commons.lang3.StringEscapeUtils.escapeJava

class Script(override val bus: MessageEventBus) extends AbstractBot {
  val workingDir = "/home/spydon/scripts/" // TODO: Read from application.conf
  val suffix = ".dsh"
  val lineFormat = "#d "

  implicit val timeOut: Timeout = 1 second

  val helpMsg =
    ">>>`$script` or `$s` runs one of the defined scripts\\n" +
    "`$script list` tells the user which scripts that exist\\n" +
    "`$script name arg1, ...argX` runs a script with arguments"

  def toCode(code: String): String = "`" + code + "`"

  def listScripts(): String = {
    val scripts = (("ls " + workingDir) !!).split("\\n").filter(_.endsWith(suffix))

    def getDesc(filename: String): Option[String] = {
      for (line <- Source.fromFile(workingDir + filename).getLines()) {
        if (line.startsWith(lineFormat)) return Some(line.stripPrefix(lineFormat))
      }
      None
    }

    val desc = scripts.map(
      script => toCode(script.stripSuffix(suffix)) + " :: " + getDesc(script).getOrElse("No Description")
    )
    desc.foldLeft(">>>")((lines, line) => lines.concat(line + "\\n"))
  }

  def isScript(name: String): Boolean = (("ls " + workingDir) !!).split("\\n").count(_ == name + suffix) == 1

  def runScript(script: String, args: List[String]): Future[String] = {
    val result = Promise[String]
    val p = workingDir + script + suffix + args.mkString(" ", " ", "")
    println(p)
    if(isScript(script)) result.success(escapeJava(p).!!.filter(_ >= ' ').trim)
    else result.success(s"`$script` does not exist")
    result.future
  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel, helpMsg)

  override def act: Receive = {
    case Command("script", List("list"), message) =>
      publish(OutboundMessage(message.channel, listScripts()))

    case Command("script", rawKeys, message) =>
      if(AdminUtil.lowAccess(rawKeys.head) || AdminUtil.hasAccess(message.user)) {
        val (user, keys) = if(rawKeys.last.contains("@U")) (rawKeys.last, rawKeys.dropRight(1))
                           else ("<@"+message.user+">", rawKeys)
        publish(OutboundMessage(message.channel, "Started running script: `" + keys.mkString(" ") + "`"))
        Future {
          runScript(keys.head, keys.tail).onComplete {
            case Success(result) => publish(OutboundMessage(message.channel, s"$result - Ping ${user}"))
            case Failure(e) => publish(OutboundMessage(message.channel, "Something failed"))
          }
        }
      } else {
        publish(OutboundMessage(message.channel, "You don't have the privilege to do that"))
      }

    case Command("s", keys, message) => act(Command("script", keys, message))
  }

}
