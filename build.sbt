organization := "lt.dvim.citywasp"
name := "kabrioletas"

resolvers += Resolver.bintrayRepo("2m", "maven")
libraryDependencies ++= Seq(
  "citywasp"          %% "citywasp-api" % "0.3",
  "com.typesafe.akka" %% "akka-stream"  % "2.5.2"
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalafmtVersion := "1.0.0-RC2"
enablePlugins(ScalafmtPlugin, JavaAppPackaging)
