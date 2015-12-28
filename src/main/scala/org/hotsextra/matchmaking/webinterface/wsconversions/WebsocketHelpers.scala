package org.hotsextra.matchmaking
package wsconversions

import org.http4s.websocket.WebsocketBits._
import org.http4s.server.websocket._
import scalaz.stream.Exchange
import scalaz.stream.Process
import scalaz.stream.process1
import scalaz.stream.Process1
import scalaz.concurrent.Task

trait WSEncoder[-A] {
  def encode(a: A): Process[Task, WebSocketFrame]
}

trait WSDecoder[+A] {
  def decode: Process1[WebSocketFrame, A]

}

object WSDecoder {
  implicit val id = fromConversion(id => id)

  def fromConversion[A](f: WebSocketFrame => A) = new WSDecoder[A] {
    def decode = process1.lift(f)
  }

  def receivedFrames = new WSDecoder[ReceivedDataFrame] {
    def decode = process1.lift((frame: WebSocketFrame) => frame match {
      case t: Text => ReceivedDataFrame.fromTextFrame(t)
      case bin: Binary => ReceivedDataFrame.fromBinFrame(bin)
      case unsupported => throw new MatchError(s"Decoding non-aggregated Text or Binary frame or control frame $unsupported not supported")
    })
  }
}

object WSEncoder {
  implicit val id = new WSEncoder[WebSocketFrame] {
    def encode(frame: WebSocketFrame) = Process.emit(frame)
  }

  def fromConversion[A](f: A => WebSocketFrame) = new WSEncoder[A] {
    def encode(a: A) = Process.emit(f(a))
  }
}

object WebsocketHelpers {
  def TWS[I, W](ex: Exchange[I, W])(implicit encoder: WSEncoder[I], decoder: WSDecoder[W]) = {
    val src = ex.read.flatMap(encoder.encode)
    val snk = ex.write.pipeIn(decoder.decode)
    WS(Exchange(src, snk))
  }

}