package de.qno.tournamentadmin

import TournamentAdmin.{nextTournaments, readCalendar}

import upickle.default.*

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class TournamentAdmin():
  def addInstance(tournamentInstance: TournamentInstance) =
    nextTournaments.enqueue(tournamentInstance)

object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  private var nextTournaments: mutable.PriorityQueue[TournamentInstance] = new mutable.PriorityQueue()

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
    val pairingAlgo = lichessArena.pairingAlgorithm
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

  def readCalendar =
    write(nextTournaments)

def maisn(): Unit =
  val jString = readCalendar
  os.write.over(TournamentAdmin.pathToResources / "calender.json", jString)
  println(jString)
  val allSeries: Array[TournamentInstance] = Array(TournamentInstance.warmUp, TournamentInstance.untitledTuesday)
  val jsonDateString = write(allSeries)
  os.write.over(TournamentAdmin.pathToResources / "series.json", jsonDateString)


