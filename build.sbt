version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.8"
val scalaTestVersion = "3.0.8"
val AkkaVersion = "2.6.19"


libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)


lazy val root =
  (project in file("."))
    .settings(name := "test-arena")
