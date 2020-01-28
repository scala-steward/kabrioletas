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

resolvers += Resolver.bintrayRepo("2m", "maven")
libraryDependencies ++= Seq(
  "citywasp" %% "citywasp-api" % "1.2",
  "com.typesafe.akka" %% "akka-stream" % "2.6.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "de.heikoseeberger" %% "akka-http-circe" % "1.30.0",
  "com.danielasfregola" %% "twitter4s" % "6.2",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
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
bintrayOrganization := Some("2m")
bintrayRepository := (if (isSnapshot.value) "snapshots" else "maven")
organizationName := "https://github.com/2m/kabrioletas/graphs/contributors"

scalafmtOnCompile := true

enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, MicrositesPlugin)
