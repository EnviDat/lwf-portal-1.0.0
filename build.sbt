name := """lwf-portal"""

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
resolvers += Resolver.url("typesafe", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)


scalaVersion := "2.11.11"

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.14"
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.0"
libraryDependencies += "com.jcraft" % "jsch" % "0.1.53"
//libraryDependencies += "org.zeroturnaround" % "zt-zip" % "1.8"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.9"
libraryDependencies += "org.apache.commons" % "commons-email" % "1.5"

libraryDependencies += specs2 % Test




