name := "isucon_internal"

organization := "com.github.takashabe"

version := "0.1"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "io.spray" %%  "spray-json" % "1.3.2",
  "org.scalaj" %% "scalaj-http" % "2.2.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "org.scala-lang" % "scala-reflect" % "2.11.2"
)

initialCommands := "import com.github.takashabe.isucon_internal._"
