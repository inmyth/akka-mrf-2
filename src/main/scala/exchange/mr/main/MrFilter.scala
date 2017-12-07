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
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsDefined
import play.api.libs.json.JsObject

object MrFilter extends App {
  implicit val system = ActorSystem("MrFilter-system")
  implicit val materializer = akka.stream.ActorMaterializer()
  import system.dispatcher

  var conf = ConfigFactory.load("application.conf");
  
  val sinkUrl     = conf.getString("filter.sink.url")
  val sourceHost  = conf.getString("filter.source.host")
  val sourcePort  = conf.getInt("filter.source.port")
  val sourcePath  = conf.getString("filter.source.path")
  
  println(s"$sourceHost:$sourcePort$sourcePath -> $sinkUrl")
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
    "sign",
    "submit_multisigned", // this cannot work with sign and submit mode
    "book_offers",
    "subscribe",
    "unsubscribe")

  val transactionList = Seq(
    "OfferCreate",
    "OfferCancel")

  def isValidRequest(request: JsValue): Boolean = {
    val validCommand = Try(commandList.contains((request \ "command").as[String])).getOrElse(false)
    val validTransaction =
      Try(transactionList.contains(
        ((request \ "tx_json") \ "TransactionType").as[String])).getOrElse(false)
    (validCommand || validTransaction)
  }


  // Flows --
  val userFlow = Flow[Message]

  val messageToJsValueFlow = Flow[Message].collect {
    case message: TextMessage.Strict => message.textStream.map(Json.parse(_)) collect {
      case jsValue if (isValidRequest(jsValue)) => jsValue
      case _ => Json.obj("command" -> "")
    } recover {
      case e: Exception =>
//        println(e)
        Json.obj("command" -> "")
    }
  }


  val jsValueToMessageFlow = Flow[Source[JsValue, _]] map { jsValueStream =>
    TextMessage.Streamed(jsValueStream.map(msg => {
      Json.stringify(msg)
    }))
  }
  
  def rippledFlow = Http().webSocketClientFlow(WebSocketRequest(sinkUrl))

  def handlerFlow =
    userFlow via
      messageToJsValueFlow via
      jsValueToMessageFlow via
      rippledFlow

  // Start listing --
  val binding = Http().bindAndHandleSync({
    case request @ HttpRequest(GET, Uri.Path(sourcePath), _, _, _) =>
      request
        .header[UpgradeToWebSocket]
        .map(_.handleMessages(handlerFlow))
        .getOrElse(HttpResponse(StatusCodes.BadRequest))
    case _ => HttpResponse(StatusCodes.NotFound)
  }, sourceHost, sourcePort)

  // Waiting --
  println(s"Press return to shutdown")
  scala.io.StdIn.readLine()
  binding.flatMap(_.unbind()).andThen({ case _ => system.terminate() })

}