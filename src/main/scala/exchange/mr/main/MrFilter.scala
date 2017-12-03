package exchange.mr.main

import scala.concurrent.Future
import scala.util.Try
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{ HttpResponse, HttpRequest, StatusCodes, Uri }
import akka.http.scaladsl.model.ws.{ Message, TextMessage, UpgradeToWebSocket, WebSocketRequest }
import akka.stream.scaladsl.{ Flow, Source }
import play.api.libs.json.{ Json, JsValue }
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import java.io.File
import akka.http.scaladsl.settings.ConnectionPoolSettings
import scala.concurrent.duration._

object MrFilter extends App {

  //  val customConf = ConfigFactory.parseString("""
  //  akka.actor.deployment {
  //    /my-service {
  //      router = round-robin-pool
  //      nr-of-instances = 3
  //    }
  //  }
  //  """)

  //  val configFile = getClass.getClassLoader.getResource("application.conf").getFile
  //  val config = ConfigFactory.parseFile(new File(configFile))

  implicit val system = ActorSystem("MrFilter-system")
  implicit val materializer = akka.stream.ActorMaterializer()
  import system.dispatcher

  var conf = ConfigFactory.load("application.conf");
  println("Server idle-timeout " + conf.getString("akka.http.server.idle-timeout"))

  println("Server linger-timeout " + conf.getString("akka.http.server.linger-timeout"))

  val commandList = Seq(
    "account_currencies",
    "account_info",
    "account_lines",
    "account_offers",
    "account_objects",
    "account_tx",
    "ledger",
    "ledger_closed",
    "ledger_current",
    "ledger_data",
    "ledger_entry",
    "ledger_request",
    "ledger_accept",
    "tx",
    "transaction_entry",
    "tx_history",
    "path_find",
    "ripple_path_find",
    "submit",
    "submit_multisigned",
    "book_offers",
    "subscribe",
    "unsubscribe"
  )

  val transactionList = Seq(
    "OfferCreate",
    "OfferCancel"
  )

  def isValidRequest(request: JsValue): Boolean = {
    val validCommand = Try(commandList.contains((request \ "command").as[String])).getOrElse(false)
    val validTransaction =
      Try(transactionList.contains(
        ((request \ "tx_json") \ "TransactionType").as[String]
      )).getOrElse(false)

    validCommand || validTransaction
  }

  // Flows --
  val userFlow = Flow[Message]

  val messageToJsValueFlow = Flow[Message] collect {
    case message: TextMessage.Strict => message.textStream.map(Json.parse(_)) collect {
      case jsValue if (isValidRequest(jsValue)) => jsValue
      case _ => Json.obj("command" -> "")
    } recover {
      case e: Exception =>
        //println(e)
        Json.obj("command" -> "")
    }
  }

  val jsValueToMessageFlow = Flow[Source[JsValue, _]] map { jsValueStream =>
    TextMessage.Streamed(jsValueStream.map(msg => { //TODO Filter "XRP" out
      Json.stringify(msg)
    }))
  }

  def rippledFlow = Http().webSocketClientFlow(WebSocketRequest("wss://rippled.mr.exchange"))

  def handlerFlow =
    userFlow via
      messageToJsValueFlow via
      jsValueToMessageFlow via
      rippledFlow

  // Start listing --
  val binding = Http().bindAndHandleSync({
    case request @ HttpRequest(GET, Uri.Path("/socket"), _, _, _) =>
      request
        .header[UpgradeToWebSocket]
        .map(_.handleMessages(handlerFlow))
        .getOrElse(HttpResponse(StatusCodes.BadRequest))
    case _ => HttpResponse(StatusCodes.NotFound)
  }, "localhost", 8080)

  // Waiting --
  println(s"Press return to shutdown")
  scala.io.StdIn.readLine()
  binding.flatMap(_.unbind()).andThen({ case _ => system.terminate() })

}