package delvian


import akka.actor.{ActorContext, ActorRef, ActorSystem, Props}
import delvian.modules._
import delvian.modules.admin.Admin
import io.scalac.slack.api.{BotInfo, Start}
import io.scalac.slack.bots.system.CommandsRecognizerBot
import io.scalac.slack.common.actors.SlackBotActor
import io.scalac.slack.common.Shutdownable
import io.scalac.slack.{BotModules, MessageEventBus, Config => SlackConfig}
import io.scalac.slack.websockets.WebSocket

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.slick.util.Logging

object BotRunner extends Shutdownable with Logging {
  val system = ActorSystem("DelvianSystem")
  val eventBus = new MessageEventBus
  val delvianBot = system.actorOf(Props(classOf[SlackBotActor], new DelvianBundle(), eventBus, this, None), "delvian")

  var botInfo: Option[BotInfo] = None

  def main(args: Array[String]) {
    logger.info("Delvian started")
    logger.info("With api key: " + SlackConfig.apiKey)

    try {
      delvianBot ! Start
    } catch {
      case e: Exception =>
        println("An unhandled exception occurred...", e)
        shutdown()
    }
  }

  sys.addShutdownHook(shutdown())

  override def shutdown(): Unit = {
    println("Tries to shutdown")
    delvianBot ! WebSocket.Release
    system.stop(delvianBot)
    val terminated = system.terminate()
    Await.result(terminated, 5 seconds)
    println("Shutdown successful.")
    sys.exit(0)
  }

  class DelvianBundle() extends BotModules {
    override def registerModules(context: ActorContext, websocketClient: ActorRef) = {
      context.actorOf(Props(classOf[CommandsRecognizerBot], eventBus), "commandProcessor")
      context.actorOf(Props(classOf[Help],                  eventBus), "help")
      context.actorOf(Props(classOf[Exit],                  eventBus), "exit")
      context.actorOf(Props(classOf[Amandine],              eventBus), "amandine")
      context.actorOf(Props(classOf[Jari],                  eventBus), "jari")
      context.actorOf(Props(classOf[Friday],                eventBus), "friday")
      context.actorOf(Props(classOf[Bet],                   eventBus), "bet")
      context.actorOf(Props(classOf[UrlSaver],              eventBus), "url")
      context.actorOf(Props(classOf[Holiday],               eventBus), "holiday")
      context.actorOf(Props(classOf[Script],                eventBus), "script")
      context.actorOf(Props(classOf[Admin],                 eventBus), "admin")
      context.actorOf(Props(classOf[Alias],                 eventBus), "alias")
    }
  }
}
