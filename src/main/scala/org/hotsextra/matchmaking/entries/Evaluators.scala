package org.hotsextra.matchmaking
package entries

import scala.collection.immutable.Queue
import scalaz.syntax.FoldableOps
import java.time.Clock
import java.time.Duration
import rating.FoldQueue
import grizzled.math.stats._

sealed trait Evaluation
case object Reject extends Evaluation
case object Accept extends Evaluation
case object Hold extends Evaluation

object Evaluators {
  implicit val fold = FoldQueue

  case class CountedEvaluator[A](count: Int, src: Evaluate[A])

  type Evaluate[A] = Queue[A] => Evaluation

  def heroleagueSoloQueueEvaluator(teamsize: Int, target: Int, threshold: Duration, pointpersec: Double)(clock: Clock): Evaluate[HeroLeagueEntry] = (pool) => {
    val count: Int = pool.size

    if (count > 2 * teamsize) Reject
    else if (count < 2 * teamsize) Hold
    else {

      //val dev: Double = totalrating.sigma

      /*
      * the following is not right.
      * something better would make use of an answer to
      * http://stats.stackexchange.com/questions/186463/distribution-of-difference-between-two-normal-distributions
      * and start with
      * val totalrating = grouprating(pool.map(_.rating))
      * val differences = pool.map(entry => entry.rating.difference(totalrating))
      */

      val dev = sampleStdDev(pool.map(_.rating.score): _*)

      val waittimes = pool.map(entry => Duration.between(entry.jointime, clock.instant()))
      val maxwait: Duration = waittimes.foldLeft(Duration.ZERO)((longest, next) => if (longest.compareTo(next) < 0) next else longest)
      val timeexceedance = maxwait.minus(threshold).getSeconds()
      val timescore = if (timeexceedance > 0) (pointpersec * timeexceedance).toInt else 0
      if ((dev - timescore) <= target) Accept
      else Reject
    }

  }

  def heroleagueMinusNSigma(n: Double)(teamsize: Int, target: Int, threshold: Duration, pointpersec: Double)(clock: Clock): Evaluate[HeroLeagueEntry] = (pool) => {
    val count: Int = pool.size

    if (count > 2 * teamsize) Reject
    else if (count < 2 * teamsize) Hold
    else {

      //val dev: Double = totalrating.sigma

      /*
      * the following is not right.
      * something better would make use of an answer to
      * http://stats.stackexchange.com/questions/186463/distribution-of-difference-between-two-normal-distributions
      * and start with
      * val totalrating = grouprating(pool.map(_.rating))
      * val differences = pool.map(entry => entry.rating.difference(totalrating))
      */

      val scores: Seq[Double] = pool.map(player => (player.rating.score - (n * player.rating.sigma)))

      val dev = sampleStdDev(scores: _*)

      val waittimes = pool.map(entry => Duration.between(entry.jointime, clock.instant()))
      val maxwait: Duration = waittimes.foldLeft(Duration.ZERO)((longest, next) => if (longest.compareTo(next) < 0) next else longest)
      val timeexceedance = maxwait.minus(threshold).getSeconds()
      val timescore = if (timeexceedance > 0) (pointpersec * timeexceedance).toInt else 0
      if ((dev - timescore) <= target) Accept
      else Reject
    }

  }

}