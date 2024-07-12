ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "TournamentAdmin",
    idePackagePrefix := Some("de.qno.tournamentadmin")
  )
libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M14"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0"
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1"