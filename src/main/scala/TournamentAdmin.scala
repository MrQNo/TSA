package de.qno.tournamentadmin

import sttp.client4.*
import com.github.nscala_time.time.Imports.*
import org.joda.time.format.ISODateTimeFormat

enum ChessPlatform:
 case chesscom, lichess

enum TournamentType:
  case swiss, arena

case class TournamentAdmin()

object TournamentAdmin:
  val teamID = "deutscher-schachbund-ev-offen"
  val token = "lip_6x0MtZl7vMFLRUVOv3iw"

case class AdminApi(base: String,
               pairingAlgorithm: String,
               createString: String,
               time: String,
               increment: String,
               duration: String,
               startdate: String
               )

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
                        )

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
                             additionalConds: Map[String, String]):
  def nextNext: TournamentInstance = 
    val response = createNextInstance(createMap)
    println(response.body)
    TournamentInstance(nextInstance.number + 1, 
      nextInstance.nextDate.plusDays(nextDays(nextInstance.pointerDays)), 
      (nextInstance.pointerTimes +1) % limits.length, 
      (nextInstance.pointerDays +1) % nextDays.length) 
    
  def createMap: Map[String, String] = 
    Map(
      "name" -> s"$nextInstance.number. $title",
      s"${apiStrings.time}" -> limits(nextInstance.pointerTimes).toString,
      s"${apiStrings.increment}" -> increments(nextInstance.pointerTimes).toString,
      s"${apiStrings.duration}" -> duration.toString,
      s"${apiStrings.startdate}" -> nextInstance.nextDate.getMillis.toString,
      s"description" -> description
    ) ++ additionalConds
    
  def createNextInstance(creationMap: Map[String, String]) = 
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

  val pairingAlgo = lichessArena.pairingAlgorithm
  val createString = ""
  
def createTournament =
  while warmUp.nextInstance.nextDate < DateTime.now().plusMonths(1)
    do
      warmUp.nextInstance = warmUp.nextNext

@main
def main: Unit =
  val fmt = ISODateTimeFormat.dateTime()
  val dt = DateTime.now()
  println(dt.toString)
  val dtstr = fmt.print(dt)
  println(dtstr)
  val dt2 = fmt.parseDateTime(dtstr)
  println(dt2.toString)

  