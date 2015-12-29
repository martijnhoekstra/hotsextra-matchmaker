package org.hotsextra.matchmaking
package webinterface

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.HttpService
//import org.http4s.Http4s._
import scalaz.concurrent.Task
import org.http4s.server.HttpService
import org.http4s.server.blaze.BlazeBuilder
//import org.http4s.websocket.WebsocketBits._
import org.http4s.dsl._
//import org.http4s.server.websocket._

import scala.concurrent.duration._

import scalaz.concurrent.Task
import scalaz.concurrent.Strategy
import scalaz.stream.async.unboundedQueue
import scalaz.stream.Process
import scalaz.stream.{ DefaultScheduler, Exchange }
import scalaz.stream.time.awakeEvery
import scalaz.stream.async.mutable.Topic
import java.time.{ Duration => JDuration }
import java.time.Clock
import scala.collection.immutable.Queue

import entries._
import process._
import scalaz.stream.async.topic
import codec.Encoders._
import argonaut._
import Argonaut._

import org.http4s.dsl._
import org.http4s.twirl._

import Evaluators._
import rating.Rating

import scalaz.stream.async.boundedQueue

object TestRunner extends App {

  implicit def clock = Clock.systemUTC

  def randentry = {
    val rand = new java.util.Random()
    HeroLeagueEntry(rand.nextInt(), Rating(rand.nextDouble() * 5000, rand.nextDouble() * 200), Clock.systemUTC.instant)
  }

  val route = HttpService {
    case GET -> Root => Ok(html.index())
    case GET -> Root / "script" / "site" => Ok(js.site())
    case GET -> Root / "css" / "site.css" => {
      Ok(servestrings.Css.site)(servestrings.Css.cssEncoder)
    }
    case GET -> Root / "queues" / "hl" / "solo" :? params => {
      import wsconversions.WebsocketHelpers.TWS
      import wsconversions.ArgonautSockets._
      import wsconversions.WSEncoder
      import wsconversions.WSDecoder
      import codec.Decoders._

      import scala.util.Try
      println("starting websocket server")

      val mevaluator = for {
        steamsize <- params.get("teamsize").flatMap(_.headOption)
        starget <- params.get("target").flatMap(_.headOption)
        streshold <- params.get("threshold").flatMap(_.headOption)
        sppm <- params.get("secondsperpoint").flatMap(_.headOption)
        teamsize <- Try { steamsize.toInt }.toOption
        target <- Try { starget.toInt }.toOption
        threshold <- Try { streshold.toLong }.toOption
        ppm <- Try { sppm.toDouble }.toOption
      } yield HeroLeagueSoloQueueEvaluator(teamsize, target, JDuration.ofSeconds(threshold), ppm)(Clock.systemUTC)

      mevaluator.map { evaluator =>
        val joinhlsolo = boundedQueue[HeroLeagueEntry](8)
        val heroleaguesolo = Pool.heroLeagueSoloQueue(joinhlsolo.dequeue, evaluator)

        val matches: Process[Task, List[HeroLeagueEntry]] = heroleaguesolo.evalMap(x => x match {
          case EndOfQueue => Task.delay(EndOfQueue).after(1.seconds)
          case x => Task.now(x)
        }).collect { case org.hotsextra.matchmaking.Accepted(q) => q.toList }

        val built = matches.map(matchbuilding.Matchbuilders.oneTwoHeroLeague _)

        println("establishing")
        //can't figure out how to get correct implicit resultion for decoder. Do it all manually
        val dj = decodeHLE(clock)
        val decoder = argonautDecoder(dj)
        val encoder = implicitly[WSEncoder[(List[HeroLeagueEntry], List[HeroLeagueEntry])]]
        val exchange = Exchange(built, joinhlsolo.enqueue)
        TWS(exchange)(encoder, decoder)
      }.getOrElse(Ok("Invalid"))
    }
  }

  BlazeBuilder.bindHttp(666)
    .withWebSockets(true)
    .mountService(route, "/matchmaking")
    .run
    .awaitShutdown()

}