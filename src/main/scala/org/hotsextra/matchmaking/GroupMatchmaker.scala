package org.hotsextra.matchmaking

import scalaz.Heap
import scalaz.Order
import scalaz.Ordering._
import java.time.Instant
import scalaz.\/
import scalaz.\/-
import scalaz.-\/
import scalaz.{ NonEmptyList => NEL }
import scala.collection.immutable.Queue
import entries._
import entries.Evaluators._

case class GroupQueue[A](small: Heap[A], large: Heap[A])
case class GroupPool[A](small: Queue[NEL[A]], large: Queue[NEL[A]])

case class GroupMatchmaker[A](traversed: Heap[NEL[A]],
    pool: Queue[NEL[A]],
    untraversed: Heap[NEL[A]],
    order: Order[NEL[A]]) {

  implicit val ord = order

  def join(party: NEL[A]) = untraversed.minimumO match {
    //gloss over edge-case optimisations: don't want to insert into pool (step calls growPool at start)
    case Some(aa) if ord(party, aa) != LT => GroupMatchmaker(traversed, pool, untraversed.insert(party), order)
    case _ => GroupMatchmaker(traversed.insert(party), pool, untraversed, order)
  }

  def accept = (pool, copy(pool = Queue.empty))

  def reset = GroupMatchmaker(Heap.Empty.apply[NEL[A]], Queue.empty, traversed.union(untraversed).insertAll(pool), ord)

  def atEnd = untraversed.isEmpty

  def initPool: GroupMatchmaker[A] = ??? //not possible for small group. Oops? (nah, Matchmaker[A]\/Matchmaker[A])

  def step(evaluate: Evaluate[NEL[A]]): (StepResult[NEL[A]], GroupMatchmaker[A]) = {
    evaluate(pool) match {
      case Accept => (Accepted(pool), copy(pool = Queue.empty).initPool)
      case Reject => {
        val (head, tail) = pool.dequeue
        def loop(untraversed: Heap[NEL[A]], traversed: Heap[NEL[A]]): (StepResult[NEL[A]], GroupMatchmaker[A]) = {
          untraversed.uncons match {
            case None => (EndOfQueue, reset)
            case Some((party, remaining)) if (party.size == head.size) => (Rejected(head), GroupMatchmaker(traversed.insert(head), tail.enqueue(party), remaining, ord))
            case Some((party, remaining)) => loop(remaining, traversed.insert(party))
          }
        }
        loop(untraversed, traversed)
      }
      case Hold => ??? //not possible in this scheme (which is worrysome)

    }

  }
}

object GroupMatchmaker {
  def empty[A](implicit ord: Order[NEL[A]]) = Matchmaker(Heap.Empty.apply[NEL[A]], Queue.empty, Heap.Empty.apply[NEL[A]], ord)
}

