import akka.actor.{ActorRef, ActorContext, Props, ActorSystem}
import io.scalac.slack.api.{BotInfo, Start}
import io.scalac.slack.bots.system.CommandsRecognizerBot
import io.scalac.slack.common.actors.SlackBotActor
import io.scalac.slack.common.{Command, UsersStorage, Shutdownable}
import io.scalac.slack.{Config => SlackConfig, BotModules, MessageEventBus}
import io.scalac.slack.websockets.{WebSocket, WSActor}
import modules._
import modules.admin.Admin

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object BotRunner extends Shutdownable {
  val system = ActorSystem("DelvianSystem")
  val eventBus = new MessageEventBus
  val delvianBot = system.actorOf(Props(classOf[SlackBotActor], new DelvianBundle(), eventBus, this, None), "delvian")

  var botInfo: Option[BotInfo] = None

  def main(args: Array[String]) {
    println("Delvian started")
    println("With api key: " + SlackConfig.apiKey)

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
    delvianBot ! WebSocket.Release
    val terminated = system.terminate()
    Await.result(terminated, Duration.Inf)
    println("Shutdown successful...")
  }

  class DelvianBundle() extends BotModules {
    override def registerModules(context: ActorContext, websocketClient: ActorRef) = {
      context.actorOf(Props(classOf[CommandsRecognizerBot], eventBus), "commandProcessor")
      context.actorOf(Props(classOf[Help],                  eventBus), "help")
      context.actorOf(Props(classOf[Friday],                eventBus), "friday")
      context.actorOf(Props(classOf[UrlSaver],              eventBus), "url")
      context.actorOf(Props(classOf[Holiday],               eventBus), "holiday")
      context.actorOf(Props(classOf[Script],                eventBus), "script")
      context.actorOf(Props(classOf[Admin],                 eventBus), "admin")
      context.actorOf(Props(classOf[Alias],                 eventBus), "alias")
    }
  }
}
