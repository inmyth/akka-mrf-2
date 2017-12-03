package exchange.mr.main

import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.HttpServerRequest
import io.vertx.core.Handler
import io.vertx.scala.core.http.ServerWebSocket
import io.vertx.scala.core.http.HttpClientOptions
import akka.actor.ActorSystem
import exchange.mr.mrf2.actor.ServerActor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import exchange.mr.mrf2.actor.StartWebSocketServer
import akka.dispatch.ExecutionContexts._
import exchange.mr.mrf2.actor.RippledActor

object VertxApp extends App {
  //    var rippledEndPoint = "rippled.mr.exchange"
  //    var vertx = Vertx.vertx()
  //    var server = vertx.createHttpServer()
  //    var cliWsOpts = HttpClientOptions()
  //      .setDefaultHost(rippledEndPoint)
  //      .setDefaultPort(443)
  //      .setSsl(true)
  //  
  //  
  //  
  //    server.requestHandler(req2Server => {
  //      var serWs = req2Server.upgrade()
  //      serWs.writeFinalTextFrame("Scala Vertx")
  //  
  //      var cliWs     = vertx.createHttpClient(cliWsOpts)
  //      cliWs.websocket("/", rippleRes => {
  //        println(s"Connected to RippleD at $rippledEndPoint")
  //        
  //  
  //  
  //      })
  //  
  //    })

  implicit val ec = global

  val system = ActorSystem("System")
  val actorServer = system.actorOf(Props(new ServerActor()))
  implicit val timeout = Timeout(25 seconds)
  val fServerWS = actorServer ! StartWebSocketServer()
  //  fServerWS.map {println}

}