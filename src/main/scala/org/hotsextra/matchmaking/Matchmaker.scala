package org.hotsextra.matchmaking

import scalaz.Zipper
import java.time.Clock
import java.time.Instant

case class QueueEntry(playerId: Int, rating: Rating, joined: Instant)

class Matchmaker(queueo: Option[Zipper[QueueEntry]], treshold: Double, matchquality: List[QueueEntry] => Clock => Double, teamsize: Int) {
  import Matchmaker._

  def reap(clock: Clock): (List[MatchResult], Matchmaker) = {
    queueo match {
      case None => (Nil, this)
      case Some(zipper) => {
        //using the zipper as a ringbuffer. This leads to a (constant factor) perf degradation.
        val haltingfocus = zipper.focus
        def rec(zipper: Zipper[QueueEntry], agg: List[MatchResult]): (List[MatchResult], Option[Zipper[QueueEntry]]) = {
          if (zipper.focus == haltingfocus) (agg, Some(zipper))
          else
            take(teamsize * 2, zipper) match {
              case None => {
                val restarted = zipper.start
                if (containsN(teamsize * 2 - 1, restarted.rights)) rec(restarted, agg)
                else (agg, Some(zipper))
              }
              case Some((item :: rest, newzippero: Option[Zipper[QueueEntry]])) => {
                if (matchquality(item :: rest)(clock) > treshold)
                  if (item == haltingfocus || newzippero.isEmpty) ((item :: rest) :: agg, newzippero)
                  else rec(newzippero.get, (item :: rest) :: agg)
                else newzippero match {
                  case None => (agg, None)
                  case Some(zip) => rec(zip, agg)
                }
              }
              case _ => throw new Error("Are you having a giggle?")
            }
        }
        val (res, next) = rec(zipper.nextC, Nil)
        (res, new Matchmaker(next, treshold, matchquality, teamsize))
      }
    }
  }

  def insert(entry: QueueEntry): Matchmaker = {
    queueo match {
      case None => new Matchmaker(Some(Zipper(Stream.empty, entry, Stream.empty)), treshold, matchquality, teamsize)
      case Some(zipper) => {
        val erating = entry.rating.rating
        def rec(zipper: Zipper[QueueEntry], insertWhen: QueueEntry => Boolean): Zipper[QueueEntry] = {
          if (insertWhen(zipper.focus)) zipper.insertLeft(entry)
          else zipper.next match {
            case Some(nz) => rec(nz, insertWhen)
            case None => zipper.insertRight(entry)
          }
        }
        val newz =
          if (zipper.focus.rating.rating > erating)
            rec(zipper, item => item.rating.rating < erating)
          else
            reverse(rec(reverse(zipper), item => item.rating.rating > erating))
        new Matchmaker(Some(newz), treshold, matchquality, teamsize)
      }
    }
  }
}

object Matchmaker {
  type MatchResult = List[QueueEntry]
  def reverse[A](zipper: Zipper[A]) = Zipper(zipper.rights, zipper.focus, zipper.lefts)
  def take[A](n: Int, zipper: Zipper[A]): Option[(List[A], Option[Zipper[A]])] = {
    //non tailrec. For our use case (10 items) this is ok.
    def rec(rem: Int, zipo: Option[Zipper[A]], agg: List[A]): Option[(List[A], Option[Zipper[A]])] = {
      if (rem < 0) throw new Error("no shenanigans")
      else if (rem == 0) Some(agg, zipo)
      else zipo.flatMap(zip => rec(rem - 1, zip.next, zip.focus :: agg))
    }
    rec(n, Some(zipper), Nil)
  }

  def containsN(n: Int, stream: Stream[_]): Boolean = {
    if (n < 0) throw new Error("no shenennigans")
    else if (n == 0) true
    else if (stream.isEmpty) n == 1 //to avoid accessing the tail of an empty stream
    else containsN(n - 1, stream.tail)
  }

}

