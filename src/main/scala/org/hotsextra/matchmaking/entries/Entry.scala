package org.hotsextra.matchmaking
package entries

import java.time.Instant
import rating.Rating

case class HeroLeagueEntry(playerid: Int, rating: Rating, jointime: Instant)
case class QuickMatchEntry(playerid: Int, rating: Rating, jointime: Instant, hero: Option[Hero])