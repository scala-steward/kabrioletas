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
  "citywasp"            %% "citywasp-api"    % "0.4",
  "com.typesafe.akka"   %% "akka-stream"     % "2.5.11",
  "com.typesafe.akka"   %% "akka-http"       % "10.1.1",
  "de.heikoseeberger"   %% "akka-http-circe" % "1.20.1",
  "com.danielasfregola" %% "twitter4s"       % "5.5",
  "org.akka-js" %% "shocon" % "0.2.1",
)

excludeDependencies ++= Seq(
  ExclusionRule("com.typesafe", "config")
)

//compile in Compile := (compile in Compile).dependsOn(shoconConcat).value

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
startYear := Some(2017)
homepage := Some(url("https://github.com/2m/kabrioletas"))
scmInfo := Some(ScmInfo(url("https://github.com/2m/kabrioletas"), "git@github.com:2m/kabrioletas.git"))
developers += Developer("contributors",
                        "Contributors",
                        "https://gitter.im/2m/kabrioletas",
                        url("https://github.com/2m/kabrioletas/graphs/contributors"))
bintrayOrganization := Some("2m")
bintrayRepository := (if (isSnapshot.value) "snapshots" else "maven")
organizationName := "https://github.com/2m/kabrioletas/graphs/contributors"

scalafmtOnCompile := true

enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, MicrositesPlugin, ShoconPlugin)
