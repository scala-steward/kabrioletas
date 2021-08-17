organization := "lt.dvim.citywasp"
name := "kabrioletas"
description := "Stay up to date with the summer ride"

organizationName := "2m"
organizationHomepage := Some(url("http://2m.lt"))

micrositeTwitter := "@kabrioletas"
micrositeGithubOwner := "2m"
micrositeGithubRepo := "kabrioletas"
micrositeBaseUrl := "/kabrioletas"
micrositeAnalyticsToken := "UA-100826420-1"
micrositeExtraMdFiles := Map(file("readme.md") -> microsites.ExtraMdFileConfig("index.md", "home"))
micrositePalette := Map(
  "brand-primary" -> "#86A19A",
  "brand-secondary" -> "#9DB095",
  "brand-tertiary" -> "#6E654B",
  "gray-dark" -> "#413C58",
  "gray" -> "#747185",
  "gray-light" -> "#B9B8C2",
  "gray-lighter" -> "#DCDBE0",
  "white-color" -> "#FFFFFF"
)

scalaVersion := "2.13.6"
libraryDependencies ++= Seq(
  "lt.dvim.citywasp"              %% "citywasp-api"       % "2.0.3",
  "com.typesafe.akka"             %% "akka-stream"        % "2.6.15",
  "com.typesafe.akka"             %% "akka-http"          % "10.2.6",
  "de.heikoseeberger"             %% "akka-http-circe"    % "1.37.0",
  "com.softwaremill.sttp.client3" %% "akka-http-backend"  % "3.3.13",
  "com.danielasfregola"           %% "twitter4s"          % "7.0",
  "org.scala-lang.modules"        %% "scala-java8-compat" % "0.9.0"
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
startYear := Some(2017)
homepage := Some(url("https://github.com/2m/kabrioletas"))
scmInfo := Some(ScmInfo(url("https://github.com/2m/kabrioletas"), "git@github.com:2m/kabrioletas.git"))
developers += Developer(
  "contributors",
  "Contributors",
  "https://gitter.im/2m/kabrioletas",
  url("https://github.com/2m/kabrioletas/graphs/contributors")
)
organizationName := "https://github.com/2m/kabrioletas/graphs/contributors"

scalafmtOnCompile := true
scalafixOnCompile := true
ThisBuild / scalafixDependencies ++= Seq(
  "com.nequissimus" %% "sort-imports" % "0.5.5"
)

enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, MicrositesPlugin)
