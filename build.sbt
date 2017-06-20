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
  "brand-primary"   -> "#86A19A",
  "brand-secondary" -> "#9DB095",
  "brand-tertiary"  -> "#6E654B",
  "gray-dark"       -> "#413C58",
  "gray"            -> "#747185",
  "gray-light"      -> "#B9B8C2",
  "gray-lighter"    -> "#DCDBE0",
  "white-color"     -> "#FFFFFF"
)

resolvers += Resolver.bintrayRepo("2m", "maven")
libraryDependencies ++= Seq(
  "citywasp"            %% "citywasp-api" % "0.3",
  "com.typesafe.akka"   %% "akka-stream"  % "2.5.2",
  "com.danielasfregola" %% "twitter4s"    % "5.1"
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
startYear := Some(2017)

scalafmtVersion := "1.0.0-RC3"
scalafmtOnCompile := true

enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, MicrositesPlugin, ScalafmtPlugin)