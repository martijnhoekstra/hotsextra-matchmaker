package org.hotsextra.matchmaking
package process

import java.time.Clock

import scala.annotation.tailrec
import entries._
import entries.Evaluators._
import scalaz.stream._
import scalaz.concurrent.Task
import scalaz.\/
import scalaz.\/-
import scalaz.-\/
import scala.collection.immutable.Queue
import entries.HeroLeagueEntry
import java.time.Duration
import entries.Evaluators.Evaluate
import scalaz.Order
import scalaz.Scalaz._
import scalaz.State
import scalaz.StateT

object Pool {
  //rest API POST matchmaker/heroleague/soloqueue {HeroLeagueEntry}
  //rest API DELETE
  //var matchmaker = Matchmaker.empty[Evaluators.HeroLeagueEntry]

  def heroLeagueSoloQueue(joins: Process[Task, HeroLeagueEntry], baseEvaluator: Evaluate[HeroLeagueEntry]): Process[Task, StepResult[HeroLeagueEntry]] = {
    //def baseEvaluator: Evaluate[HeroLeagueEntry] = HeroLeagueSoloQueueEvaluator(5, 200, Duration.ofMinutes(6), 0.3)(Clock.systemUTC)

    implicit val entryorder = Order.orderBy((entry: HeroLeagueEntry) => entry.rating.score).reverseOrder

    type Produce[A] = State[Matchmaker[HeroLeagueEntry], A]

    val all: Process[Task, Produce[StepResult[HeroLeagueEntry]]] = {
      def joinstep(entry: HeroLeagueEntry): Produce[Unit] = State { mm =>
        {
          val joined = mm.join(entry)
          (joined, ())
        }
      }
      val scanstep: Produce[StepResult[HeroLeagueEntry]] = State { mm => mm.step(baseEvaluator).swap }
      val scan = Process.constant(scanstep, 1)
      val joinspu: Process[Task, Produce[Unit]] = joins.map(joinstep)
      val joinsproduce: Process[Task, Produce[StepResult[HeroLeagueEntry]]] = joinspu.map(x => x.map(xx => Continue))

      wye(joinsproduce, scan)(wye.merge)

    }
    all.stateScan(Matchmaker.empty[HeroLeagueEntry])(id => id)
  }
}