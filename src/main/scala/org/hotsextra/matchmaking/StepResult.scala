package org.hotsextra.matchmaking

import scala.collection.immutable.Queue

sealed trait StepResult[+A]
case object Continue extends StepResult[Nothing]
case object EndOfQueue extends StepResult[Nothing]
case class Rejected[A](item: A) extends StepResult[A]
case class Accepted[A](items: Queue[A]) extends StepResult[A]