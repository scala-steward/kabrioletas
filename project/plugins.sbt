addSbtPlugin("com.dwijnand"      % "sbt-dynver"            % "1.3.0")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"          % "1.1.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt-coursier" % "1.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"            % "2.0.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"   % "1.2.0")
addSbtPlugin("com.47deg"         % "sbt-microsites"        % "0.6.0" exclude("com.lihaoyi", "sourcecode_2.10")) // scalafmt brings in 2.11 version of sourcecode
