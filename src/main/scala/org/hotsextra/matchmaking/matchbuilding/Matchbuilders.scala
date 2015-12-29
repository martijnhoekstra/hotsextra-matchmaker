package org.hotsextra.matchmaking.matchbuilding

import scala.annotation.tailrec
import org.hotsextra.matchmaking.entries.HeroLeagueEntry
import org.hotsextra.matchmaking.entries.QuickMatchEntry
import org.hotsextra.matchmaking.entries._

object Matchbuilders {

  def splitlist[A](as: List[A]): (List[A], List[A]) = {
    @tailrec
    def rec(larger: List[A], smaller: List[A], remaining: List[A]): (List[A], List[A]) = {
      remaining match {
        case Nil => (larger, smaller)
        case head :: tail => rec(head :: smaller, larger, tail)
      }
    }
    rec(Nil, Nil, as)
  }
  /**
   * This matchbuilder simply divides the players from highest to lowest MMR to one side each
   */
  def interleavingHeroLeague(entries: List[HeroLeagueEntry]) = splitlist(entries)

  def oneTwoHeroLeague(entries: List[HeroLeagueEntry]) = entries match {
    case List(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) => {
      (List(p1, p4, p5, p7, p10),
        List(p2, p3, p6, p8, p9))
    }
    case _ => interleavingHeroLeague(entries)
  }

  /**
   * This matchbuilder only takes team comp in to account.
   * It's only three gurantees are:
   * 1. Don't put more than one of the same hero in a team
   * 2. If the number of entries with some role is even, divide them evenly
   * 3. If there is an odd number of supports and a random, give the random
   * to the team with the fewest supports.
   */
  def basicQuickmatch(entries: List[QuickMatchEntry]) = {
    val byhero = entries.groupBy(entry => entry.hero)
    if (byhero.exists(x => x._2.size > 2)) throw new Error("dafuq did you give me a list with three or more the same heroes?") else ()

    val (dups, nodupsm) = byhero.partition(x => x._2.size > 1)

    val (startl, startr) = dups.map { case (_, entries) => List(entries(0), entries(1)) }.transpose match {
      case a :: b :: Nil => (a, b)
    }

    val nodups = nodupsm.values.flatten.toList

    val (supports, nosupport) = nodups.partition(entry => entry.hero.map(_.role) == Some(Support))

    val (s1, s2) = splitlist(supports)

    val (supported1, supported2) = ((startl ++ s1), (startr ++ s2))

    val (random, norandom) = nosupport.partition(entry => entry.hero == None)

    val (r1, r2) = splitlist(random)

    val (rl, rr) = ((supported1 ++ r1).toList, (supported2 ++ r2).toList)

    val byrole = norandom.groupBy(entry => entry.hero.map(_.role))

    def addsplits[A](p1: (List[A], List[A]), p2: (List[A], List[A])) = {
      def sl(l1: List[A], l2: List[A]) = if (l1.size < l2.size) (l1, l2) else (l2, l1)

      val (s1, l1) = sl(p1._1, p1._2)
      val (s2, l2) = sl(p2._1, p2._2)
      (s1 ++ l2, l1 ++ s2)
    }

    byrole.foldLeft((rl, rr)) { case (t, entries) => addsplits(t, splitlist(entries._2)) }
  }
}