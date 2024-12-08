package de.qno.tournamentadmin

import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*

enum LichesSystem(system: String) derives ReadWriter:
  case ARENA extends LichesSystem("arena")

enum LichessStatus(status: Integer) derives ReadWriter:
  case CREATED extends LichessStatus(10)
  case STARTED extends LichessStatus(20)
  case FINISHED extends LichessStatus(30)

enum LichessVariantKey(variant: String) derives ReadWriter:
  case STANDARD extends LichessVariantKey("standard")
  case CHESS960 extends LichessVariantKey("chess960")
  case CRAZYHOUSE extends LichessVariantKey("crazyhouse")
  case ANTICHESS extends LichessVariantKey("antichess")
  case ATOMIC extends LichessVariantKey("atomic")
  case HORDE extends LichessVariantKey("horde")
  case KINGOFTHEHILL extends LichessVariantKey("kingOfTheHill")
  case RACINGKINGS extends LichessVariantKey("racingKings")
  case THREECHECK extends LichessVariantKey("threeCheck")
  case FROMPOSITION extends LichessVariantKey("fromPosition")

enum LichessPerfType(perf: String) derives ReadWriter:
  case ULTRABULLET extends LichessPerfType("ultraBullet")
  case BULLET extends LichessPerfType("bullet")
  case BLITZ extends LichessPerfType("blitz")
  case RAPID extends LichessPerfType("rapid")
  case CLASSICAL extends LichessPerfType("classical")
  case CORRESPONDENCE extends LichessPerfType("correspondence")
  case CHESS960 extends LichessPerfType("chess960")
  case CRAZYHOUSE extends LichessPerfType("crazyhouse")
  case ANTICHESS extends LichessPerfType("antichess")
  case ATOMIC extends LichessPerfType("atomic")
  case HORDE extends LichessPerfType("horde")
  case KINGOFTHEHILL extends LichessPerfType("kingOfTheHill")
  case RACINGKINGS extends LichessPerfType("racingKings")
  case THREECHECK extends LichessPerfType("threeCheck")

case class LichessClock(limit: Integer, increment: Integer) derives ReadWriter

case class LichessVariant(key: LichessVariantKey, name: String, short: String)
object LichessVariant:
  implicit val lat: ReadWriter[LichessVariant] = macroRW

case class LichessArenaPerf(key: String, name: String, position: String, icon: String) derives ReadWriter

case class LichessArenaRatingObj(perf: LichessPerfType, rating: Integer)
object LichessArenaRatingObj:
  implicit val laro: ReadWriter[LichessArenaRatingObj] = macroRW

case class LichessMinRated(nb: Integer) derives ReadWriter

sealed trait LichessArenaPosition derives ReadWriter
case class LichessThematic(eco: String, name: String, fen: String, url: String) extends LichessArenaPosition
case class LichessCustomPosition(name: String, fen: String) extends LichessArenaPosition

case class LichessSchedule(freq: String, speed: String) derives ReadWriter

case class LichessTeamBattle(teams: Array[String], nbLeaders: Integer)
object LichessTeamBattle:
  implicit val ltb: ReadWriter[LichessTeamBattle] = macroRW

case class LichessWinner(id: String, name: String = "") derives ReadWriter

case class LichessArenaTournamentList(id: String, createdBy: String, system: LichesSystem, minutes: Integer, clock: LichessClock, rated: Boolean, fullName: String,
                                      nbPlayers: Integer, variant: LichessVariant, startsAt: Integer, finishesAt: Integer, status: LichessStatus, secondsToStart: Integer,
                                      hasMaxRating: Boolean, maxRating: LichessArenaRatingObj, minRating: LichessArenaRatingObj, minRatedGames: LichessMinRated, onlyTitled: Boolean, teamMember: String,
                                      privat: Boolean, position: LichessArenaPosition, schedule: LichessSchedule, teamBattle: LichessTeamBattle, winner: LichessWinner)
object LichessArenaTournamentList:
  implicit val latl: ReadWriter[LichessArenaTournamentList] = macroRW

case class LichessArenaTournament(created: LichessArenaTournamentList,
                                  started: LichessArenaTournamentList,
                                  finished: LichessArenaTournamentList)
object LichessArenaTournament:
  implicit val lat: ReadWriter[LichessArenaTournament] = macroRW

case class LichessApi(teamId: String, ltoken: String):
  /**
   * Create a Lichess Arena.
   * All Parameters are Strings.
   * @param name Name of Tournament
   * @param time Clock initial time in MINUTES
   * @param increment Clock increment in seconds
   * @param minutes How long tournament lasts, in minutes
   * @param startDate Start Date as Timestamp in milliseconds
   * @param description Tournament description
   * @return ID of created tournament
   */
  def createArena(name: String, time: String, increment: String, minutes: String, startDate: String, description: String): String =
    val creationMap = Map(
      "name" -> name,
      "clockTime" -> time,
      "clockIncrement" -> increment,
      "minutes" -> minutes,
      "startDate" -> startDate,
      "description" -> description,
      "conditions.teamMember.teamId" -> teamId
    )
    ujson.read(basicRequest
      .auth.bearer(ltoken)
      .body(creationMap)
      .post(uri"https://lichess.org/api/tournament")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body)("id").str

  /**
   * Create a Lichess swiss tournament.
   * All Params are Strings.
   * @param name Name of Tournament
   * @param time Clock initial time in SECONDS
   * @param increment Clock increment in seconds
   * @param nbRounds Maximum number of rounds
   * @param startDate Start Date as Timestamp in milliseconds
   * @param description Tournament description
   * @param maxRating Maximum rating to join; defaults at empty
   * @return ID of created tournament
   */
  def createSwiss(name: String, time: String, increment: String, nbRounds: String, startDate: String, description: String, maxRating: String = ""): String =
    val creationMap = Map(
      "name" -> name,
      "clock.limit" -> time,
      "clock.increment" -> increment,
      "nbRounds" -> nbRounds,
      "startsAt" -> startDate,
      "description" -> description,
      "additionalConds" -> teamId,
      "conditions.maxRating.rating" -> maxRating,
      "conditions.playYourGames" -> "true"
    )
    val composedUrl: String = s"https://lichess.org/api/swiss/new/$teamId"
    ujson.read(basicRequest
      .auth.bearer(ltoken)
      .body(creationMap)
      .post(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body)("id").str

  /**
   * Get an Iterator over a nlJSON list of Lichess Arena tournaments of a team 
   * @param team the ID of a team, defaults to DSB
   * @return an Iterator[String] containing one JSON tournament description per line.
   */
  def getArena(): LichessArenaTournament =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/arena"
    basicRequest
      .auth.bearer(ltoken)
      .get(uri"$composedUrl")
      .response(asJson[LichessArenaTournament].getRight)
      .send(DefaultSyncBackend())
      .body

  /**
   * Get an Iterator over a nlJSON list of Lichess Swiss tournaments of a team 
   * @param team the ID of a team, defaults to DSB
   * @return an Iterator[String] containing one JSON tournament description per line.
   */
  def getSwiss(): Iterator[String] =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/swiss"
    basicRequest
      .auth.bearer(ltoken)
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body.linesIterator

  /**
   * Send a message to all members of my team.
   * @param text the message text
   * @return true if success, false or Exception otherwise
   */
  def sendMessage(text: String): Boolean =
    val composedUrl = s"https://lichess.org/team/$teamId/pm-all"
    val resp = ujson.read(basicRequest
      .auth.bearer(ltoken)
      .body(Map("message" -> text))
      .post(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body)
    resp("ok").bool