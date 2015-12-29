package org.hotsextra.matchmaking

import scalaz.Heap
import scalaz.Order
import scalaz.Ordering._
import java.time.Instant
import scalaz.\/
import scalaz.\/-
import scalaz.-\/
import scala.collection.immutable.Queue
import entries._
import entries.Evaluators._

case class Matchmaker[A](traversed: Heap[A], pool: Queue[A], untraversed: Heap[A], order: Order[A]) {

  implicit val ord = order

  def join(a: A) = untraversed.minimumO match {
    //gloss over edge-case optimisations: don't want to insert into pool (step calls growPool at start)
    case Some(aa) if ord(a, aa) != LT => Matchmaker(traversed, pool, untraversed.insert(a), order)
    case _ => Matchmaker(traversed.insert(a), pool, untraversed, order)
  }

  def growPool = untraversed.uncons map { case (h, u) => Matchmaker(traversed, pool.enqueue(h), u, order) }

  def reject = pool.dequeueOption map { case (head, tail) => (head, Matchmaker(traversed.insert(head), tail, untraversed, ord)) }

  def accept = (pool, copy(pool = Queue.empty))

  def reset = Matchmaker(Heap.Empty.apply[A], Queue.empty, traversed.union(untraversed).insertAll(pool), ord)

  def atEnd = untraversed.isEmpty

  def step(evaluate: Evaluate[A]): (StepResult[A], Matchmaker[A]) = {

    growPool.map(mm => evaluate(mm.pool) match {
      case Accept => {
        val (p, accmm) = mm.accept
        (Accepted(p), accmm)
      }
      case Reject => {
        //if growPool suceeded, reject.get is guaranteed to succeed as well
        val (r, rejmm) = reject.get
        (Rejected(r), rejmm)
      }
      case Hold => (Continue, mm)
    }).getOrElse(
      {
        (EndOfQueue, reset)
      })
  }
}

object Matchmaker {
  def empty[A](implicit ord: Order[A]) = Matchmaker(Heap.Empty.apply[A], Queue.empty, Heap.Empty.apply[A], ord)
}

