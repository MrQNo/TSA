package de.qno.tournamentadmin

import sttp.client4.*
import com.github.nscala_time.time.Imports.*
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*

case class TournamentAdmin()

object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read(pathToResources / "token.txt")
  var nextTournaments: Map[LocalDate, (Int, ChessPlatform)] = Map[LocalDate, (Int, ChessPlatform)]()

  enum ChessPlatform derives ReadWriter:
    case chesscom, lichess

  enum TournamentType derives ReadWriter:
    case swiss, arena

  object RWgivens:
    private val fmt = ISODateTimeFormat.dateTime()
    given ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
      dt => dt.toString,
      dtstr => fmt.parseDateTime(dtstr.str)
    )

  import RWgivens.given

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
      createString = s"/new/$TournamentAdmin.teamID",
      time = "clock.limit",
      increment = "clock.increment",
      duration = "nbRounds",
      startdate = "startsAt"
    )

  def createTournament(): Unit =
    while warmUp.nextInstance.nextDate < DateTime.now().plusMonths(1)
    do
      warmUp.nextInstance = warmUp.nextNext

@main
def main(): Unit =
  val jString = write(TournamentAdmin.nextTournaments)
  os.write.over(TournamentAdmin.pathToResources / "calender.json", jString)
  println(jString)
  val allSeries: Array[TournamentSeries] = Array(warmUp, untitledTuesday)
  val jsonDateString = write(allSeries)
  os.write.over(TournamentAdmin.pathToResources / "series.json", jsonDateString)


