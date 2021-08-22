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

import scala.concurrent.duration._

import com.danielasfregola.twitter4s.entities.AccessToken
import com.danielasfregola.twitter4s.entities.ConsumerToken
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import moped.annotations._
import moped.cli._
import moped.commands._
import moped.json.JsonDecoder
import moped.json.JsonEncoder
import moped.json.Result
import sttp.model.Uri

import lt.dvim.citywasp.api.Model.AppVersion
import lt.dvim.citywasp.kabrioletas.BuildInfo
import lt.dvim.citywasp.kabrioletas.Supervisor

case class Twitter(
    @Description("Twitter consumer token")
    consumer: ConsumerToken = ConsumerToken("", ""),
    @Description("Twitter access token")
    access: AccessToken = AccessToken("", "")
)

@Description("Searches and reports fun cars")
case class Kabrioletas(
    @Description("If provided, will send tweets, otherwise just prints them")
    realRun: Boolean = false,
    @Description("App version to send")
    appVersion: AppVersion = "9.9.9",
    @Description("List of backend URIs to check")
    backendUris: List[Uri] = Nil,
    @Description("Service ID to check for")
    serviceId: Int = 96,
    @Description("How often to poll for cars")
    pollInterval: FiniteDuration = 3.minutes,
    @Description("OpenCageData access key")
    openCageDataKey: String = "",
    @Description("Twitter credentials")
    @Inline
    twitter: Twitter = Twitter(),
    app: Application = Application.default
) extends BaseCommand {
  def runAsFuture() = {
    implicit val ec = scala.concurrent.ExecutionContext.global
    Supervisor.run(this).map(_ => 0)
  }
}

object Kabrioletas {
  implicit val uriEncoder = JsonEncoder.stringJsonEncoder.contramap[Uri](_.toString)
  implicit val uriDecoder = JsonDecoder.stringJsonDecoder.map(Uri.unsafeParse)

  implicit val finiteDurationEncoder = JsonEncoder.stringJsonEncoder.contramap[FiniteDuration](_.toString)
  implicit val finiteDurationDecoder = JsonDecoder.stringJsonDecoder.flatMap { v =>
    Result.fromUnsafe { () =>
      Duration(v) match {
        case fd: FiniteDuration => fd
        case _                  => throw new IllegalArgumentException("'" + v + "' is not a finite duration.")
      }
    }
  }

  implicit def refinedStringEncoder[A] = JsonEncoder.stringJsonEncoder.contramap[Refined[String, A]](_.toString)
  implicit def refinedStringDecoder[A] = JsonDecoder.stringJsonDecoder.flatMap { v =>
    val refined: Either[String, AppVersion] = refineV(v)
    Result.fromUnsafe(() => refined.fold(err => throw new Error(err), identity))
  }

  implicit val consumerTokenCodec = moped.macros.deriveCodec(ConsumerToken("", ""))
  implicit val accessTokenCodec = moped.macros.deriveCodec(AccessToken("", ""))
  implicit val twitterCodec = moped.macros.deriveCodec(Twitter())

  implicit lazy val parser = CommandParser.derive(Kabrioletas())
  lazy val app = Application
    .fromName(
      binaryName = "kabrioletas",
      version = BuildInfo.version,
      commands = List(
        CommandParser[Kabrioletas],
        CommandParser[HelpCommand],
        CommandParser[VersionCommand]
      )
    )
    .withIsSingleCommand(true)
  def main(args: Array[String]): Unit =
    System.exit(app.run(args.toList))
}
