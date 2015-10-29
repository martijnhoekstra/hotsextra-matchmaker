package org.hotsextra.matchmaking

import java.time.Clock
import java.time.Instant
import scalaz.Order

object Matchmaking {

  def maketeams[A](players: List[A]): (List[A], List[A]) = {

    def rec(t1: List[A], t2: List[A], remain: List[A]): (List[A], List[A]) = remain match {
      case head :: tail => rec(t2, head :: t1, tail)
      case Nil => (t1, t2)
    }
    players match {
      case p1 :: p2 :: p3 :: p4 :: tail => rec(p2 :: p3 :: Nil, p1 :: p4 :: Nil, tail)
      case ps => rec(Nil, Nil, ps)
    }
  }

  val firstattempt: List[QueueEntry] => Clock => Double =
    (qe: List[QueueEntry]) => (cl: Clock) => {
      //time weighs as 1 + e^(x-8) with x in minutes

      val teams = maketeams(qe)
      ???

    }
}