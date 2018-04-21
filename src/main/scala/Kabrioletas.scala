/*
 * Copyright 2017 https://github.com/2m/kabrioletas/graphs/contributors
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

import java.time.{Duration, Instant}

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import citywasp.api._
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Tweet}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._

import scala.concurrent.duration.{Duration => _, _}
import scala.util.{Failure, Random}

object CabrioCheck {
  case object DoTheCheck
  case class ParkedCars(cars: Seq[ParkedCar])
  case class LastTweetAndCar(tweets: RatedData[Seq[Tweet]], car: Option[ParkedCar])
  case class CarWithLocation(car: ParkedCar, location: OpenCageData.Location)
}

class CabrioCheck extends Actor with ActorLogging {
  import CabrioCheck._
  import context.dispatcher

  val config       = context.system.settings.config.getConfig("citywasp")
  implicit val cw  = RemoteCityWasp(config)
  implicit val sys = context.system
  implicit val mat = ActorMaterializer()

  val twitter                 = TwitterRestClient()
  final val OpenCageDataKey   = context.system.settings.config.getString("opencagedata.key")
  final val CardModelToSearch = context.system.settings.config.getString("kabrioletas.model")

  var lastTweetAt: Instant = _

  context.system.scheduler.schedule(0.seconds, 5.minutes, self, DoTheCheck)

  override def preStart() = {
    resetLastTweetTimer()
  }

  def receive = {
    case DoTheCheck =>
      log.info("Starting to look for a wanted car.")
      CityWasp.session.pipeTo(self)
    case session: Session => session.loginChallenge.pipeTo(self)
    case challenge: LoginChallenge =>
      log.info("Got login challenge. Ready to go!")
      challenge.login.pipeTo(self)
    case login: LoggedIn =>
      log.info("Successfully logged in!")
      login.parkedCars.map(ParkedCars).pipeTo(self)
    case ParkedCars(cars) =>
      val car = cars.find(_.model.equalsIgnoreCase(CardModelToSearch))
      log.info(s"Car search resulted in $car")
      twitter.homeTimeline(count = 1).map(LastTweetAndCar(_, car)).pipeTo(self)
    case LastTweetAndCar(RatedData(_, Nil), None) =>
      log.info(s"No tweets and no car. Keep on searching...")
      if (Duration.between(Instant.now, lastTweetAt).toDays.abs >= 1) {
        tweetAboutSearch().pipeTo(self)
      }
    case LastTweetAndCar(RatedData(_, Nil), Some(car)) =>
      log.info(s"Found a car. It is gonna be a great first tweet!")
      reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
    case LastTweetAndCar(RatedData(_, tweet :: _), Some(car)) =>
      if (!tweet.text.contains("ready")) {
        log.info(
          s"Found a car [$car] and last tweet was about taken car [${tweet.text}]. Let's tell the world about the car we just found!")
        reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
      } else if (tweet.coordinates.isDefined && math.abs(tweet.coordinates.get.coordinates.head - car.lon) > 0.001 && math
                   .abs(tweet.coordinates.get.coordinates.tail.head - car.lat) > 0.001) {
        log.info(
          s"Found a car [$car] and last tweet was about a parked car [${tweet.text}], but it was on a different place. Let tell the world about the car we just found!")
        reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
      } else {
        log.info(s"Found a car [$car] and last tweet was about a parked car [${tweet.text}] at the same place.")
      }
    case LastTweetAndCar(RatedData(_, tweet :: _), None) =>
      if (tweet.text.contains("ready")) {
        log.info(s"No car and last tweet was about a parked car [${tweet.text}]. Tweet about taken car.")
        tweetAboutNoCar().pipeTo(self)
      } else {
        log.info(s"No car and we know it!")
        if (Duration.between(Instant.now, lastTweetAt).toDays.abs >= 1) {
          tweetAboutSearch().pipeTo(self)
        }
      }
    case cwl @ CarWithLocation(car, location) =>
      log.info(s"Reverse geocoded car location to $location")
      tweetAbout(cwl).pipeTo(self)
    case t: Tweet =>
      log.info("Tweet success.")
      resetLastTweetTimer()
    case m =>
      log.error(s"Unhandled message $m")
      m match {
        case Failure(ex) =>
          log.error(ex.getMessage)
          ex.printStackTrace()
        case akka.actor.Status.Failure(ex) =>
          log.error(ex.getMessage)
          ex.printStackTrace()
      }
  }

  def reverseGeocodeCarLocation(car: ParkedCar)(implicit sys: ActorSystem) = {
    import OpenCageData._

    Http()
      .singleRequest(
        HttpRequest(uri = Uri("http://api.opencagedata.com/geocode/v1/json").withQuery(
          Query("q" -> s"${car.lat},${car.lon}", "key" -> OpenCageDataKey))))
      .flatMap(resp => Unmarshal(resp.entity).to[Location])
  }

  def tweetAbout(carWithLocation: CarWithLocation) = {
    val CarWithLocation(car, location) = carWithLocation

    val cityDescription = for {
      suburb <- location.suburb.map(_ + ", ").orElse(Some(""))
      city   <- location.city
    } yield s"$suburb$city"

    val locationDescription = cityDescription.orElse(location.town).map(location => s" in $location").getOrElse("")

    twitter.createTweet(
      status =
        f"\uD83D\uDE95\uD83D\uDE95\uD83D\uDE95 Parked and ready for a new adventure$locationDescription. Pick me up! https://www.google.com/maps?q=${car.lat}%.6f,${car.lon}%.6f",
      latitude = Some(car.lat.toLong),
      longitude = Some(car.lon.toLong),
      display_coordinates = true
    )
  }

  def tweetAboutNoCar() = {
    twitter.createTweet(status =
      s"\uD83D\uDD1C\uD83D\uDD1C\uD83D\uDD1C I am on a ride right now. Will let you know when I am free! (${Random.alphanumeric.take(6).mkString})")
  }

  def tweetAboutSearch() = {
    twitter.createTweet(status =
      s"🔎🔎🔎 There has been no available car for quite some time now. Nevertheless, I keep on searching. Stay tuned! (${Random.alphanumeric.take(6).mkString})")
  }

  def resetLastTweetTimer() =
    lastTweetAt = Instant.now
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

object OpenCageData extends FailFastCirceSupport {
  case class Location(suburb: Option[String], city: Option[String], town: Option[String])

  implicit val decodeLocation: Decoder[Location] = new Decoder[Location] {
    final def apply(c: HCursor): Decoder.Result[Location] =
      for {
        suburb <- c.downField("results").downArray.first.downField("components").downField("suburb").as[Option[String]]
        city   <- c.downField("results").downArray.first.downField("components").downField("city").as[Option[String]]
        town   <- c.downField("results").downArray.first.downField("components").downField("town").as[Option[String]]
      } yield Location(suburb, city, town)
  }
}
