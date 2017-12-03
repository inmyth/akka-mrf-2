package exchange.mr.main

import com.neovisionaries.ws.client.WebSocket

trait OnWSConnectedListener {

  def onConnected(ws: WebSocket, arg1: Map[String, List[String]])

}