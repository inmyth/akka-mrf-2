package exchange.mr.mrf2.actor

import io.vertx.scala.core.http.ServerWebSocket

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef

case class ServerWSActorStart()

class ServerWSActor(ws: ServerWebSocket) extends Actor {

  private var rippledActor: Option[ActorRef] = None

  def receive = {

    case ServerWSActorStart() => {
      import scala.concurrent._
      import ExecutionContext.Implicits.global
      rippledActor = Some(context.actorOf(Props(new RippledActor())))
      rippledActor.get ! StartRippledSinkWs(ws.binaryHandlerID())
    }

    case RippledResponse(handlerId: String, data: String) => {
      ws.writeFinalTextFrame(ws.binaryHandlerID() + " " + data)

    }

    case _ => println("ServerWSActor message not recognized")

  }

  ws.frameHandler(buffer => {
    println(buffer.textData())

    ws.writeFinalTextFrame(ws.binaryHandlerID() + " " + buffer.textData())
    if (!rippledActor.isEmpty) {
      rippledActor.get ! buffer.textData()
    }

  })

}