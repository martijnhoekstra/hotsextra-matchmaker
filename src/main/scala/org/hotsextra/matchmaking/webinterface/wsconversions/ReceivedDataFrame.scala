package org.hotsextra.matchmaking
package wsconversions

import org.http4s.websocket.WebsocketBits._

sealed trait ReceivedDataFrame {
  def data: Array[Byte]
}

case class ReceivedTextFrame(data: Array[Byte]) extends ReceivedDataFrame {
  import java.nio.charset.StandardCharsets.UTF_8
  val str = new String(data, UTF_8)
}

case class ReceivedBinaryFrame(data: Array[Byte]) extends ReceivedDataFrame

object ReceivedDataFrame {
  implicit def fromTextFrame(textframe: Text) = ReceivedTextFrame(textframe.data)
  implicit def fromBinFrame(binaryframe: Binary) = ReceivedBinaryFrame(binaryframe.data)
}