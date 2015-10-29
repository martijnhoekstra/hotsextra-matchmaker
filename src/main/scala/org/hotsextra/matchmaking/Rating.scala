package org.hotsextra.matchmaking

import scalaz.NonEmptyList
import scalaz.std.AllInstances._

case class Rating(rating: Double, σ: Double, τ: Double) {
  //convenience accessors
  def sigma = σ
  def tau = τ
}

object Rating {
  //a first proposal. Biased towards worst player.
  def teamrating(ratings: NonEmptyList[Rating]): Rating = {
    val sorted = ratings.sortBy(r => r.rating)
    sorted.tail.foldLeft(sorted.head)((agg, n) => Rating(agg.rating + n.rating / 2, math.sqrt((agg.sigma * agg.sigma) + (n.sigma * n.sigma)) / 2, 0))
  }
}