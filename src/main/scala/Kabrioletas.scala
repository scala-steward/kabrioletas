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

import scala.concurrent.Future
import scala.concurrent.duration.{Duration => _, _}
import scala.util.{Failure, Random}

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe

import cats.implicits._
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.Tweet
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe._
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.model.{Uri => SttpUri}
import sttp.tapir.client.sttp.SttpClientInterpreter

import lt.dvim.citywasp.api.Model._
import lt.dvim.citywasp.api._
import lt.dvim.citywasp.kabrioletas.ResponseOps._

object CabrioCheck {
  case object DoTheCheck
  case object Tweeted
  case class ParkedCars(cars: List[Car])
  case class LastTweetAndCar(tweets: List[Tweet], car: Option[Car])
  case class CarWithLocation(car: Car, location: OpenCageData.Location)
}

class CabrioCheck(config: Kabrioletas) extends Actor with ActorLogging {
  import CabrioCheck._
  import context.dispatcher

  implicit val sys = context.system
  val backend = AkkaHttpBackend.usingActorSystem(sys)
  val twitter = TwitterRestClient(config.twitter.consumer, config.twitter.access)

  var lastTweetAt: Instant = _

  context.system.scheduler.scheduleAtFixedRate(0.seconds, config.pollInterval, self, DoTheCheck)

  override def preStart() =
    resetLastTweetTimer()

  def receive = {
    case DoTheCheck =>
      log.info("Starting to look for a wanted car.")
      allCars().pipeTo(self)
      ()
    case ParkedCars(cars) =>
      log.info(s"Currently total ${cars.size} cars available.")
      val car = cars.find(_.serviceId == config.serviceId)
      log.info(s"Car search resulted in $car")
      twitter.homeTimeline(count = 1).map(timeline => LastTweetAndCar(timeline.data.toList, car)).pipeTo(self)
      ()
    case LastTweetAndCar(Nil, None) =>
      log.info(s"No tweets and no car. Keep on searching...")
      if (Duration.between(Instant.now, lastTweetAt).toDays.abs >= 1) {
        tweetAboutSearch().pipeTo(self)
      }
      ()
    case LastTweetAndCar(Nil, Some(car)) =>
      log.info(s"Found a car. It is gonna be a great first tweet!")
      reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
      ()
    case LastTweetAndCar(tweet :: _, Some(car)) =>
      if (!tweet.text.contains("ready")) {
        log.info(
          s"Found a car [$car] and last tweet was about taken car [${tweet.text}]. Let's tell the world about the car we just found!"
        )
        reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
      } else if (
        tweet.coordinates.isDefined && math.abs(
          tweet.coordinates.get.coordinates.head - car.long.toDouble
        ) > 0.001 && math
          .abs(tweet.coordinates.get.coordinates.tail.head - car.lat.toDouble) > 0.001
      ) {
        log.info(
          s"Found a car [$car] and last tweet was about a parked car [${tweet.text}], but it was on a different place. Let tell the world about the car we just found!"
        )
        reverseGeocodeCarLocation(car).map(CarWithLocation(car, _)).pipeTo(self)
      } else {
        log.info(s"Found a car [$car] and last tweet was about a parked car [${tweet.text}] at the same place.")
      }
      ()
    case LastTweetAndCar(tweet :: _, None) =>
      if (tweet.text.contains("ready")) {
        log.info(s"No car and last tweet was about a parked car [${tweet.text}]. Tweet about taken car.")
        tweetAboutNoCar().pipeTo(self)
      } else {
        log.info(s"No car and we know it!")
        if (Duration.between(Instant.now, lastTweetAt).toDays.abs >= 1) {
          tweetAboutSearch().pipeTo(self)
        }
      }
      ()
    case cwl @ CarWithLocation(_, location) =>
      log.info(s"Reverse geocoded car location to $location")
      tweetAbout(cwl).pipeTo(self)
      ()
    case Tweeted =>
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
        case _ => log.error(m.toString)
      }
  }

  def allCars() =
    config.backendUris.map(cars(config.appVersion)).combineAll.map(ParkedCars.apply)

  def cars(appVersion: AppVersion)(uri: SttpUri) = {
    val params = Params.default.copy(appVersion = appVersion, country = Country.fromUri(uri))
    val carsRequest = SttpClientInterpreter().toRequest(Api.CarsLive.GetAvailableCars, Some(uri)).apply(params)
    carsRequest.send(backend).flatMap(_.toFuture)
  }

  def reverseGeocodeCarLocation(car: Car)(implicit sys: ActorSystem) = {
    import OpenCageData._

    Http()
      .singleRequest(
        HttpRequest(
          uri = Uri("http://api.opencagedata.com/geocode/v1/json")
            .withQuery(Query("q" -> s"${car.lat},${car.long}", "key" -> config.openCageDataKey))
        )
      )
      .flatMap(resp => Unmarshal(resp.entity).to[Location])
  }

  def tweetAbout(carWithLocation: CarWithLocation) = {
    val CarWithLocation(car, location) = carWithLocation

    val cityDescription = for {
      suburb <- location.suburb.map(_ + ", ").orElse(Some(""))
      city <- location.city
    } yield s"$suburb$city"

    val locationDescription = cityDescription.orElse(location.town).map(location => s" in $location").getOrElse("")
    val tweet =
      f"\uD83D\uDE95\uD83D\uDE95\uD83D\uDE95 Parked and ready for a new adventure$locationDescription. Pick me up! https://www.google.com/maps?q=${car.lat}%.6f,${car.long}%.6f ($randomMarker)"

    if (!config.realRun) {
      log.info(s"Would tweet: $tweet")
    } else {
      twitter.createTweet(
        status = tweet,
        latitude = Some(car.lat.toLong),
        longitude = Some(car.long.toLong),
        display_coordinates = true
      )
    }
    Future.successful(Tweeted)
  }

  def tweetAboutNoCar() = {
    val tweetText =
      s"\uD83D\uDD1C\uD83D\uDD1C\uD83D\uDD1C I am on a ride right now. Will let you know when I am free! ($randomMarker)"
    if (!config.realRun) {
      log.info(s"Would tweet: $tweetText")
    } else {
      twitter.createTweet(
        status = tweetText
      )
    }
    Future.successful(Tweeted)
  }

  def tweetAboutSearch() = {
    val tweetText =
      s"🔎🔎🔎 There has been no available car for quite some time now. Nevertheless, I keep on searching. Stay tuned! ($randomMarker)"
    if (!config.realRun) {
      log.info(s"Would tweet: $tweetText")
    } else {
      twitter.createTweet(
        status = tweetText
      )
    }
    Future.successful(Tweeted)
  }

  def randomMarker =
    Random.alphanumeric.take(6).mkString

  def resetLastTweetTimer() =
    lastTweetAt = Instant.now
}

class Supervisor(config: Kabrioletas) extends Actor {
  override def supervisorStrategy =
    OneForOneStrategy(loggingEnabled = true) { case _ =>
      Resume
    }

  override def preStart() = {
    context.actorOf(Props(new CabrioCheck(config)), "cabrioCheck")
    ()
  }

  def receive = { case _ =>
  }
}

object Supervisor {
  def run(config: Kabrioletas) = {
    val sys = ActorSystem("Kabrioletas")
    sys.actorOf(Props(new Supervisor(config)), "supervisor")
    sys.whenTerminated
  }
}

object OpenCageData extends FailFastCirceSupport {
  case class Location(suburb: Option[String], city: Option[String], town: Option[String])

  implicit val decodeLocation: Decoder[Location] = new Decoder[Location] {
    final def apply(c: HCursor): Decoder.Result[Location] =
      for {
        suburb <- c.downField("results").downArray.downField("components").downField("suburb").as[Option[String]]
        city <- c.downField("results").downArray.downField("components").downField("city").as[Option[String]]
        town <- c.downField("results").downArray.downField("components").downField("town").as[Option[String]]
      } yield Location(suburb, city, town)
  }
}
