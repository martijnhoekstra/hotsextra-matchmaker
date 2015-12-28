package org.hotsextra.glicko

import scalaz.NonEmptyList
import scalaz.std.AllInstances._
import scala.annotation.tailrec

trait RatingSystem {
  def τ: Double
  def tau = τ
}
case class Rating(rating: Double, RD: Double, σ: Double) {
  def sigma = σ
  def r = rating
  lazy val µ = (r - 1500) / 173.7178
  def mu = µ
  lazy val φ = RD / 173.7178
  def psi = φ
  lazy val g = 1 / math.sqrt(1 + (3 * φ * φ / (math.Pi * math.Pi)))

}

trait GameResult {
  def teammates: List[Rating]
  def opposingteam: Rating
  def win: Boolean
  def contribution: Double
  def score: Double = if (win) contribution else 0
}

object Rating {
  //tweakable
  def teamrating(ratings: NonEmptyList[Rating]): Rating = {
    val sorted = ratings.sortBy(r => r.rating)
    sorted.tail.foldLeft(sorted.head)((agg, n) => Rating(agg.rating + n.rating / 2, math.sqrt((agg.sigma * agg.sigma) + (n.sigma * n.sigma)) / 2, 0))
  }

  def ratingupdate(player: Rating, matches: List[GameResult], τ: Double): Rating = {
    val tau = τ

    def playingteam(game: GameResult) = teamrating(NonEmptyList[Rating](player, game.teammates: _*))

    def E(opponent: Rating) = 1 / (1 + math.exp(-opponent.g * (player.µ - (opponent.µ))))

    val v = {
      val opponents = matches.map(m => m.opposingteam)
      def vn(opponent: Rating) = {
        val EE = E(opponent)
        opponent.g * opponent.g * EE * (1 - EE)
      }
      1 / (opponents.map(vn _).sum)
    }

    val ∆ = {
      def deltaround(result: GameResult): Double = {
        val individualresult = result.score
        result.opposingteam.g * (individualresult - E(result.opposingteam))
      }

      matches.map(deltaround _).sum
    }

    val ∆∆ = ∆ * ∆
    val φφ = player.φ * player.φ

    val σnew = {
      val a = math.log(player.σ * player.σ)
      val ε = 0.000001
      def f(x: Double) = {
        val expx = math.exp(x)
        val n1: Double = expx * (∆∆ - φφ - (v * v) - expx)
        val d1: Double = 2 * {
          val dd = (φφ + v + expx)
          dd * dd
        }
        (n1 / d1) - ((x - a) / (τ * τ))
      }

      @tailrec
      def rec(A: Double, B: Double, ffa: Double => Double): Double = {
        if ((B - A) > -ε && (B - A) < ε) math.exp(A / 2)
        else {
          val fa = ffa(A)
          val fb = f(B)
          val C = A + (fa * (A - B) / (fa - fb))
          if ((f(C) * fb) < 0) rec(B, C, f)
          else rec(A, C, _ => fa / 2)

        }
      }
      val bstart: Double = {
        def rec(k: Int): Double = {
          val check = a - (k * τ)
          if (f(check) < 0) rec(k + 1)
          else check
        }
        if (∆∆ > (φφ + v)) math.log(∆∆ - φφ - v)
        else (rec(1))
      }
      rec(a, bstart, f)
    }

    val φstar = math.sqrt(φφ + (σnew * σnew))

    val φprime = 1 / math.sqrt((1 / (φstar * φstar)) + (1 / v))

    val µprime: Double = {
      def round(matchresult: GameResult): Double =
        matchresult.opposingteam.g * (matchresult.score - E(matchresult.opposingteam))

      val rounds = matches.map(round _).sum
      player.µ + (φprime * φprime * rounds)
    }

    val rprime = (173.178 * µprime) + 1500

    val rdPrime: Double = (173.178 + φprime)

    Rating(rprime, rdPrime, σnew)

  }

}
