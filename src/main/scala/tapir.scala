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

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import sttp.client3.Response
import sttp.model.StatusCode
import sttp.tapir.DecodeResult

object ResponseOps {
  implicit def responseToResponseOps[Resp](
      response: Response[DecodeResult[Either[String, Resp]]]
  ) = new ResponseOps(response)
}

class ResponseOps[Resp](response: Response[DecodeResult[Either[String, Resp]]]) {
  def toFutureOption: Future[Option[Resp]] = response.code match {
    case StatusCode.NotFound => Future.successful(None)
    case _                   => Future.fromTry(extractDecoded().map(Some.apply))
  }

  def toFuture: Future[Resp] = Future.fromTry(extractDecoded())

  private def extractDecoded() = response.body.flatMap(r => DecodeResult.fromOption(r.toOption)) match {
    case DecodeResult.Value(v)   => Success(v)
    case f: DecodeResult.Failure => Failure(new Exception(s"TapirFailure: ${f.toString()} when parsing $response"))
  }
}
