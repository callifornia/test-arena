enablePlugins(JavaAppPackaging, AshScriptPlugin)
version := "version_broker"
scalaVersion := "2.13.8"
val scalaTestVersion = "3.0.8"
val AkkaVersion = "2.6.19"


libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3")

// some docker settings
dockerBaseImage := "openjdk:8-jre-alpine"
packageName in Docker := "event-converter"

lazy val root =
  (project in file("."))
    .settings(name := "test-arena")
