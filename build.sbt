ThisBuild / version := "1.0.0-rc1"
ThisBuild / organization := "de.qno"
ThisBuild / scalaVersion := "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "TournamentAdmin",
    idePackagePrefix := Some("de.qno.tournamentadmin")
  )
libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M14"
libraryDependencies += "com.softwaremill.sttp.client4" %% "upickle" % "4.0.0-M14"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0"
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.10.2"

lazy val app = (project in file("app"))
  .settings(
    assembly / mainClass := Some("de.qno.tournamentAdmin.TournamentAdmin")
  )

lazy val utils = (project in file("utils"))
  .settings(
    assembly / assemblyJarName := "TSA.jar"
  )