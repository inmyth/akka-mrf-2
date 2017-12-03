package exchange.mr.mrf2.actor

import akka.actor.Actor

import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.VertxOptions

case class StartWebSocketServer()

case class WebSocketReady()

class ServerActor extends Actor {

  //  private var ws: Option[ServerWebSocket] = None
  private var mainSender: Option[ActorRef] = None
  private var rippleActors: scala.collection.mutable.Map[String, Option[ActorRef]] = scala.collection.mutable.Map()
  //  private var wsList = new ListBuffer[Option[ServerWS]]()

  import scala.concurrent._
  import ExecutionContext.Implicits.global

  def receive = {

    case StartWebSocketServer() => {
      mainSender = Some(sender)
      implicit val timeout = Timeout(25 seconds)
      var vopts = VertxOptions().setBlockedThreadCheckInterval(3601000);
      var server = Vertx.vertx(vopts).createHttpServer()

      server.requestHandler(request => {

        //        wsList +=  Some(new ServerWS(request.upgrade()))

        //        println(wsList.size)
        var actor = context.actorOf(Props(new ServerWSActor(request.upgrade())))
        actor ! ServerWSActorStart()
        //        var ws =  Some(request.upgrade())
        //        wsList ++ ws       
        //
        //        println(ws.get.binaryHandlerID())
        //        var handlerId = ws.get.binaryHandlerID()
        //        
        //        if (!rippleActors.contains(handlerId)) {
        //          rippleActors += handlerId -> Some(context.actorOf(Props[RippledActor]))            
        //          rippleActors(handlerId).get ! StartRippledSinkWs(handlerId)
        //        }
        //                        mainSender.map(_ ! "Web Socket Established")

        //        
        //        
        //        ws.get.frameHandler(buffer => {        
        //          rippleActors(ws.get.binaryHandlerID()).get! buffer.textData()
        ////          var future = rippleActor.get ? buffer.textData()
        ////          future.map(result => {
        ////              ws.get.writeFinalTextFrame(result.toString())
        ////
        ////          })
        //        })
        //
        //        //          val fServerWS = actor ? StartWebSocketServer()
        //        //  fServerWS.map {println}
      })

      server.listen(8080)

    }

    case RippledResponse(handlerId: String, data: String) => {

      //      ws.get.writeFinalTextFrame(data)
    }

    case _ => println("ServerActor message not recognized")
  }

}