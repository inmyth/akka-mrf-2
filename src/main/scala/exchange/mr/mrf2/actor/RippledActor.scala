package exchange.mr.mrf2.actor

import akka.actor.Actor
import io.vertx.scala.core.http.HttpClientOptions
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.HttpClient
import io.vertx.scala.core.http.WebSocket
import akka.actor.ActorRef

case class StartRippledSinkWs(handlerId: String)
case class RippledResponse(handlerId: String, data: String)

class RippledActor extends Actor {
  var rippledEndPoint = "rippled.mr.exchange"
  var opts = HttpClientOptions()
    .setDefaultHost(rippledEndPoint)
    .setDefaultPort(443)
    .setSsl(true)

  var ws: Option[WebSocket] = None
  var actorSender: Option[ActorRef] = None
  var handlerId: Option[String] = None

  def receive = {

    case StartRippledSinkWs(handlerId: String) => {
      this.handlerId = Some(handlerId)
      actorSender = Some(sender)

      val client = Some(Vertx.vertx().createHttpClient(opts))

      client.get.websocket("/", (websocket: io.vertx.scala.core.http.WebSocket) => {

        websocket.frameHandler(buffer => {
          //         actorSender.map(_ ! buffer.textData())
          actorSender.get ! RippledResponse(this.handlerId.get, buffer.textData())
        })

        ws = Some(websocket)

        //        client.get.frameHandler((frame: io.vertx.scala.core.http.WebSocketFrame) => {
        //          println("Received a frame of size!")
        //        })

      })

    }

    case raw: String => {
      if (!ws.isEmpty) {
        println("Rippledactor case raw" + raw)
        ws.get.writeFinalTextFrame(raw)

        //      	    	wscli.handler(cliBuf -> {
        //      	    		wsSer.writeFinalTextFrame(cliBuf.getString(0, cliBuf.length()));
        //      	    		
        //      	    	});

      } else {

      }

    }

    case _ => println("RippledActor message not recognized")

  }

}