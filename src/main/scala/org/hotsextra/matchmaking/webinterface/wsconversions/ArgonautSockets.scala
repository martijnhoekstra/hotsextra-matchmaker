package org.hotsextra.matchmaking
package wsconversions

import scalaz.\/
import scalaz.\/-
import argonaut.JsonParser
import argonaut.DecodeJson
import argonaut.EncodeJson
import scalaz.stream.Process1

import org.http4s.websocket.WebsocketBits._

object ArgonautSockets {
  implicit def argonautDecoder[A](implicit jdec: DecodeJson[A]) = new WSDecoder[A] {
    def decode: Process1[WebSocketFrame, A] = {
      def dec(str: String): \/[Any, A] = for {
        json <- JsonParser.parse(str)
        a <- jdec.decodeJson(json).result
      } yield a

      val text: Process1[WebSocketFrame, String] = WSDecoder.receivedFrames.decode collect {
        case text: ReceivedTextFrame => text.str
      }

      text map (dec _) collect {
        case \/-(x) => x
      }

    }
  }

  implicit def argonautEncoder[A](implicit jenc: EncodeJson[A]) = WSEncoder.fromConversion((a: A) => Text(jenc.encode(a).nospaces))

}
