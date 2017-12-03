package exchange.mr.main

import akka.http.scaladsl.Http

object Entry extends App {

  import system.dispatcher
  implicit val system = akka.actor.ActorSystem("Entry-system")
  implicit val materializer = akka.stream.ActorMaterializer()

  val biningFuture = Http()

  //  def route = pat

}