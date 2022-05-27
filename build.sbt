version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.8"
val scalaTestVersion = "3.0.8"
libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test
)
lazy val root = (project in file("."))
  .settings(
    name := "test-arena"
  )
