import AssemblyKeys._

name := "isucon_internal"

organization := "com.github.takashabe"

version := "0.1"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.scalaj" %% "scalaj-http" % "2.2.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.11.2",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
)

initialCommands := "import com.github.takashabe.isucon_internal._"

assemblySettings
