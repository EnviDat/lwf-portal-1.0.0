name := """lwf-portal"""

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
resolvers += Resolver.url("typesafe", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)


scalaVersion := "2.12.6"

libraryDependencies += jdbc
libraryDependencies += ehcache
libraryDependencies += ws
libraryDependencies += guice
libraryDependencies += openId
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "org.playframework.anorm" %% "anorm-akka" % "2.6.0"
libraryDependencies += "com.jcraft" % "jsch" % "0.1.53"
//libraryDependencies += "org.zeroturnaround" % "zt-zip" % "1.8"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.9"
libraryDependencies += "org.apache.commons" % "commons-email" % "1.5"
libraryDependencies += "jcifs" % "jcifs" % "1.3.17"
//libraryDependencies += "org.samba.jcifs" % "jcifs" % "1.3.17-kohsuke-1"



libraryDependencies += specs2 % Test





