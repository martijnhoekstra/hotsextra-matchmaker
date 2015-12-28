package org.hotsextra.matchmaking
package rating

import scala.collection.immutable.Queue
import scalaz.Monoid
import scalaz.Foldable
import scalaz.Functor

object RatingConvolutionMonoid extends Monoid[Rating] {
  def zero = Rating(0, 0)
  def append(a1: Rating, a2: => Rating): Rating = a1.convolution(a2)
}

object FoldQueue extends Foldable[Queue] with Functor[Queue] {
  def foldMap[A, B](fa: Queue[A])(f: A => B)(implicit F: Monoid[B]): B = {
    fa.foldLeft(F.zero)((agg, next) => F.append(agg, f(next)))
  }

  /**Right-associative fold of a structure. */
  def foldRight[A, B](fa: Queue[A], z: => B)(f: (A, => B) => B): B = {
    fa.foldRight(z)((a, b) => f(a, b))
  }

  def map[A, B](fa: Queue[A])(f: A => B) = fa.map(f)
}

case class Rating(score: Double, sigma: Double) {
  def convolution(that: Rating) = {
    val sprime = math.sqrt((sigma * sigma) + (that.sigma * that.sigma))
    val muprime = score + that.score
    Rating(muprime, sprime)
  }
}

object Rating {

  def grouprating[F[_]](ratings: F[Rating])(implicit fold: Foldable[F]) = {
    import scalaz.syntax.foldable._
    implicit val qf = FoldQueue
    implicit val rmonoid = RatingConvolutionMonoid
    val sum = ratings.sumr
    val count = ratings.foldRight(0)((_, i) => i + 1)
    Rating(sum.score / count, sum.sigma / count)
  }
}