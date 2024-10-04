import sbtassembly.AssemblyPlugin.autoImport.*
enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

ThisBuild / version := "1.0.0-rc2"
ThisBuild / organization := "de.qno"
ThisBuild / scalaVersion := "3.5.1"

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

libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M14"
libraryDependencies += "com.softwaremill.sttp.client4" %% "upickle" % "4.0.0-M14"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0"
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.10.2"

