package org.hotsextra.matchmaking
package rating

import scala.collection.immutable.Queue

case class NormalDistribution(mu: Double, sig: Double) extends Function1[Double, Double] {
  def apply(x: Double) = {
    val exp = -(((x - mu) * (x - mu)) / (2 * sig * sig))
    val denom = sig * math.sqrt(2 * math.Pi)
    math.exp(exp) / denom
  }

  def convolution(that: NormalDistribution) = {
    val sprime = math.sqrt((sig * sig) + (that.sig * that.sig))
    NormalDistribution(mu + that.mu, sprime)
  }

  def difference(that: NormalDistribution) = {
    val sprime = math.sqrt((sig * sig) + (that.sig * that.sig))
    NormalDistribution(mu - that.mu, sprime)
  }
}

