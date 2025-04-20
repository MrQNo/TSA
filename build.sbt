enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

ThisBuild / version := "2.0.0-beta4"
ThisBuild / organization := "de.qno"
ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name := "TournamentAdmin",
    idePackagePrefix := Some("de.qno.tournamentadmin"),
    Compile / mainClass := Some("de.qno.tournamentadmin.main"),
    assembly / mainClass := Some("de.qno.tournamentadmin.main"),
    assembly / assemblyJarName := "TSA.jar",
    Universal / packageName :=  "de.qno.tournamentadmin",
    Universal / maintainer := "qno-github@qno.de"
  )

libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.3"
libraryDependencies += "com.softwaremill.sttp.client4" %% "upickle" % "4.0.3"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "3.0.0"
libraryDependencies += "com.lihaoyi" %% "upickle" % "4.1.0"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4"

