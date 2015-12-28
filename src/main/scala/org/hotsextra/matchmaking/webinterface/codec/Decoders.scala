package org.hotsextra.matchmaking
package webinterface
package codec

import argonaut._
import entries.HeroLeagueEntry
import rating.Rating
import java.time.Clock

object Decoders {

  implicit def decodeRating: DecodeJson[Rating] = DecodeJson(cur => for {
    value <- (cur --\ "value").as[Double]
    deviation <- (cur --\ "uncertainty").as[Double]
  } yield Rating(value, deviation))

  implicit def decodeHLE(implicit clock: Clock): DecodeJson[HeroLeagueEntry] = DecodeJson(cur => for {
    rating <- (cur --\ "rating").as[Rating]
    player <- (cur --\ "playerid").as[Int]
  } yield HeroLeagueEntry(player, rating, clock.instant()))
}