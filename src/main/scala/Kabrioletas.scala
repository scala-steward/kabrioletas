/*
 * Copyright 2017 2m
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lt.dvim.citywasp.kabrioletas

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.pipe
import citywasp.api._
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Tweet}

import scala.concurrent.duration._
import scala.util.Random

object CabrioCheck {
  case object DoTheCheck
  case class ParkedCars(cars: Seq[ParkedCar])
  case class LastTweetAndCar(tweets: RatedData[Seq[Tweet]], car: Option[ParkedCar])
}

class CabrioCheck extends Actor with ActorLogging {
  import CabrioCheck._
  import context.dispatcher

  val config      = context.system.settings.config.getConfig("citywasp")
  implicit val cw = RemoteCityWasp(config)

  val twitter = TwitterRestClient()

  context.system.scheduler.schedule(0.seconds, 5.minutes, self, DoTheCheck)

  def receive = {
    case DoTheCheck =>
      log.info("Starting to look for a cabrio.")
      CityWasp.session.pipeTo(self)
    case session: Session => session.loginChallenge.pipeTo(self)
    case challenge: LoginChallenge =>
      log.info("Got login challenge. Ready to go!")
      challenge.login.pipeTo(self)
    case login: LoggedIn =>
      log.info("Successfully logged in!")
      login.parkedCars.map(ParkedCars).pipeTo(self)
    case ParkedCars(cars) =>
      val car = cars.find(_.brand.equalsIgnoreCase("porsche"))
      log.info(s"Car search resulted in $car")
      twitter.homeTimeline(count = 1).map(LastTweetAndCar(_, car)).pipeTo(self)
    case LastTweetAndCar(RatedData(_, Nil), None) =>
      log.info(s"No tweets and no car. Keep on searching...")
    case LastTweetAndCar(RatedData(_, Nil), Some(car)) =>
      log.info(s"Found a car. It is gonna be a great first tweet!")
      tweetAbout(car)
    case LastTweetAndCar(RatedData(_, tweet :: _), Some(car)) =>
      if (!tweet.text.contains("ready")) {
        log.info(
          s"Found a car [$car] and last tweet was about taken car [${tweet.text}]. Let's tell the world about the car we just found!")
        tweetAbout(car).pipeTo(self)
      } else if (tweet.coordinates.isDefined && math.abs(tweet.coordinates.get.coordinates.head - car.lon) > 0.001 && math
                   .abs(tweet.coordinates.get.coordinates.tail.head - car.lat) > 0.001) {
        log.info(
          s"Found a car [$car] and last tweet was about a parked car [${tweet.text}], but it was on a different place. Let tell the world about the car we just found!")
        tweetAbout(car).pipeTo(self)
      } else {
        log.info(s"Found a car [$car] and last tweet was about a parked car [${tweet.text}] at the same place.")
      }
    case LastTweetAndCar(RatedData(_, tweet :: _), None) =>
      if (tweet.text.contains("ready")) {
        log.info(s"No car and last tweet was about a parked car [${tweet.text}]. Tweet about taken car.")
        tweetAboutNoCar().pipeTo(self)
      } else {
        log.info(s"No car and we know it!")
      }
    case t: Tweet =>
      log.info("Tweet success.")
    case m =>
      log.error(s"Unhandled message $m")
  }

  def tweetAbout(car: ParkedCar) = {
    twitter.createTweet(
      status =
        f"\uD83D\uDE95\uD83D\uDE95\uD83D\uDE95 Parked and ready for a new adventure. Pick me up! https://www.google.com/maps?q=${car.lat}%.6f,${car.lon}%.6f",
      latitude = Some(car.lat.toLong),
      longitude = Some(car.lon.toLong),
      display_coordinates = true
    )
  }

  def tweetAboutNoCar() = {
    twitter.createTweet(status =
      s"\uD83D\uDD1C\uD83D\uDD1C\uD83D\uDD1C I am on a ride right now. Will let you know when I am free! (${Random.alphanumeric.take(6).mkString})")
  }
}

class Supervisor extends Actor {
  override def supervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case t => Resume
  }

  override def preStart() = {
    context.actorOf(Props[CabrioCheck], "cabrioCheck")
  }

  def receive = {
    case _ =>
  }
}

object Kabrioletas extends App {

  val sys = ActorSystem("Kabrioletas")
  sys.actorOf(Props[Supervisor], "supervisor")

}
