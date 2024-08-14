package de.qno.tournamentadmin

import TournamentAdmin.nextTournaments

import upickle.default.*

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class TournamentAdmin()

object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  var nextTournaments: ListBuffer[String] = new ListBuffer()

  enum ChessPlatform derives ReadWriter:
    case chesscom, lichess

  enum TournamentType derives ReadWriter:
    case swiss, arena

  case class AdminApi(base: String,
                 pairingAlgorithm: String,
                 createString: String,
                 time: String,
                 increment: String,
                 duration: String,
                 startdate: String
                 ) derives ReadWriter
  
  object AdminApi:
    val pairingAlgo: String = lichessArena.pairingAlgorithm
    val createString = ""

    object lichessArena extends AdminApi (
      base = "https://lichess.org/api",
      pairingAlgorithm = "/tournament",
      createString = "",
      time = "clockTime",
      increment = "clockIncrement",
      duration = "minutes",
      startdate = "startDate"
    )
  
    object lichessSwiss extends AdminApi (
      base = "https://lichess.org/api",
      pairingAlgorithm = "/swiss",
      createString = s"/new/${TournamentAdmin.teamID}",
      time = "clock.limit",
      increment = "clock.increment",
      duration = "nbRounds",
      startdate = "startsAt"
    )
