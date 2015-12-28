package org.hotsextra.matchmaking
package webinterface
package codec

import argonaut._
import argonaut.Json._
import entries.HeroLeagueEntry
import rating.Rating

object Encoders {

  implicit def encodeRating: EncodeJson[Rating] = new EncodeJson[Rating] {
    def encode(r: Rating) = Json.obj(
      ("value", jNumber(r.score)),
      ("uncertainty", jNumber(r.sigma))
    )
  }

  implicit def encodeHLE: EncodeJson[HeroLeagueEntry] = new EncodeJson[HeroLeagueEntry] {
    def encode(hle: HeroLeagueEntry) = {
      Json.obj(
        ("playerid", jNumber(hle.playerid)),
        ("rating", encodeRating(hle.rating)),
        ("joined", jString(hle.jointime.toString))
      )
    }
  }
}