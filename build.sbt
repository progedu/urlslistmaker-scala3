import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "urlslistmaker-scala3"
  )

val PekkoVersion = "1.1.3"
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "com.squareup.okhttp3" % "okhttp" % "4.12.0",
  "com.typesafe" % "config" % "1.4.3",
  "ch.qos.logback" % "logback-classic" % "1.5.18"
)