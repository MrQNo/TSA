package de.qno.tournamentadmin

import sttp.client4.*
import com.github.nscala_time.time.Imports.*
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*

enum ChessPlatform derives ReadWriter:
 case chesscom, lichess

enum TournamentType derives ReadWriter:
  case swiss, arena

case class TournamentAdmin()

object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read(pathToResources / "token.txt")
  var nextTournaments: Map[LocalDate, (Int, ChessPlatform)] = Map[LocalDate, (Int, ChessPlatform)]()

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

case class TournamentInstance( number: Int,
                          nextDate: DateTime,
                          pointerTimes: Int,
                          pointerDays: Int
                        ) derives ReadWriter

case class TournamentSeries (platform: ChessPlatform, 
                             tournamentType: TournamentType,
                             apiStrings: AdminApi,
                             title: String,
                             duration: Int,
                             var nextInstance: TournamentInstance,
                             description: String,
                             limits: Array[Int],
                             increments: Array[Int],
                             nextDays: Array[Int],
                             additionalConds: Map[String, String]) derives ReadWriter:
  def nextNext: TournamentInstance = 
    val response = createNextInstance(createMap)
    println(response.body)
    TournamentInstance(nextInstance.number + 1, 
      nextInstance.nextDate.plusDays(nextDays(nextInstance.pointerDays)), 
      (nextInstance.pointerTimes +1) % limits.length, 
      (nextInstance.pointerDays +1) % nextDays.length) 
    
  private def createMap: Map[String, String] =
    Map(
      "name" -> s"$nextInstance.number. $title",
      s"${apiStrings.time}" -> limits(nextInstance.pointerTimes).toString,
      s"${apiStrings.increment}" -> increments(nextInstance.pointerTimes).toString,
      s"${apiStrings.duration}" -> duration.toString,
      s"${apiStrings.startdate}" -> nextInstance.nextDate.getMillis.toString,
      s"description" -> description
    ) ++ additionalConds
    
  private def createNextInstance(creationMap: Map[String, String]) =
    basicRequest
      .body(creationMap)
      .post(uri"${apiStrings.base}${apiStrings.pairingAlgorithm}${apiStrings.createString}")
      .response(asString)
      .send(DefaultSyncBackend())
    
val warmUp = TournamentSeries(platform = ChessPlatform.lichess, 
  tournamentType = TournamentType.arena,
  apiStrings = lichessArena,
  title = "LiLa-Warm-Up",
  duration = 55,
  nextInstance = TournamentInstance(number = 96,
    nextDate = new DateTime(2024, 8, 5, 19, 1, 0, DateTimeZone.forID("Europe/Berlin")), 
    pointerTimes = 2, 
    pointerDays = 1),
  description = "Warm-Up-Arena für die Lichess Liga (immer mit der gleichen Bedenkzeit)",
  limits = Array(3, 3, 5),
  increments = Array(0, 2, 0),
  nextDays = Array(3, 4),
  additionalConds = Map(
    "conditions.teamMember.teamId" -> TournamentAdmin.teamID
  ))

val untitledTuesday = TournamentSeries(platform = ChessPlatform.lichess,
  tournamentType = TournamentType.swiss,
  apiStrings = lichessSwiss,
  title = "Titelloser Dienstag",
  duration = 11,
  nextInstance = TournamentInstance(number = 50,
    nextDate = new DateTime(2024, 8, 13, 20, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 0,
    pointerDays = 0),
  description = "11 Runden Schweizer System 3+2 für Spieler unter Blitzwertung 2200",
  limits = Array(180),
  increments = Array(2),
  nextDays = Array(7),
  additionalConds = Map(
    "conditions.maxRating.rating" -> "2200",
    "conditions.playYourGames" -> "true"
  )

)

  val pairingAlgo = lichessArena.pairingAlgorithm
  val createString = ""

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


