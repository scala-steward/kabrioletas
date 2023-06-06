name := "kabrioletas"
description := "Stay up to date with the summer ride"

scalaVersion := "2.13.10"
libraryDependencies ++= Seq(
  "lt.dvim.citywasp"              %% "citywasp-api"       % "2.0.4",
  "com.typesafe.akka"             %% "akka-stream"        % "2.6.20",
  "com.typesafe.akka"             %% "akka-http"          % "10.2.10",
  "de.heikoseeberger"             %% "akka-http-circe"    % "1.39.2",
  "com.softwaremill.sttp.client3" %% "akka-http-backend"  % "3.8.15",
  "com.danielasfregola"           %% "twitter4s"          % "8.0",
  "org.scala-lang.modules"        %% "scala-java8-compat" % "1.0.2",
  "org.scalameta"                 %% "moped"              % "0.1.11"
)

sonatypeProfileName := "lt.dvim"

buildInfoKeys := Seq[BuildInfoKey](version)
buildInfoPackage := "lt.dvim.citywasp.kabrioletas"

nativeImageOptions ++= List(
  "--verbose",
  "--no-fallback",
  "--initialize-at-build-time",
  "--allow-incomplete-classpath",
  "--report-unsupported-elements-at-runtime",
  "--initialize-at-run-time=scala.util.Random$"
)
nativeImageVersion := "21.2.0"
nativeImageAgentOutputDir := (Compile / resourceDirectory).value / "META-INF" / "native-image" / organization.value / name.value

inThisBuild(
  Seq(
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    startYear := Some(2017),
    homepage := Some(url("https://github.com/2m/kabrioletas")),
    developers += Developer(
      "contributors",
      "Contributors",
      "https://gitter.im/2m/general",
      url("https://github.com/2m/kabrioletas/graphs/contributors")
    ),
    organization := "lt.dvim.citywasp",
    organizationName := "https://github.com/2m/kabrioletas/graphs/contributors",
    scalafmtOnCompile := true,
    scalafixOnCompile := true,
    scalafixDependencies ++= Seq(
      "com.nequissimus" %% "sort-imports" % "0.6.1"
    ),
    versionScheme := Some("semver-spec")
  )
)

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

enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, MicrositesPlugin, BuildInfoPlugin, NativeImagePlugin)
