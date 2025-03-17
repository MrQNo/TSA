package de.qno.tournamentadmin
package lichess

import org.joda.time.*
import org.joda.time.format.*
import upickle.default.*

object LichessInternalDataTypes:
  enum LichesSystem(system: String) derives ReadWriter:
    case ARENA extends LichesSystem("arena")
    case SWISS extends LichesSystem("swiss")

  enum LichessPerfType(perf: String)derives ReadWriter:
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

  case class LichessArenaPerf(key: String, name: String, position: Integer, icon: String)derives ReadWriter

  sealed trait LichessArenaPosition derives ReadWriter
  case class LichessThematic(eco: String, name: String, fen: String, url: String) extends LichessArenaPosition
  case class LichessCustomPosition(name: String, fen: String) extends LichessArenaPosition

  case class LichessArenaRatingObj(perf: LichessPerfType, rating: Integer)
  object LichessArenaRatingObj:
    implicit val laro: ReadWriter[LichessArenaRatingObj] = macroRW

  case class LichessArenaTournamentListEntry(id: String, createdBy: String, system: String, minutes: Integer, clock: LichessClock, rated: Boolean, fullName: String,
                                             nbPlayers: Integer, variant: LichessVariant, startsAt: Integer, finishesAt: Integer, status: Integer, perf: LichessArenaPerf, secondsToStart: Integer = 0,
                                             hasMaxRating: Boolean = false, maxRating: LichessArenaRatingObj = LichessArenaRatingObj(LichessPerfType.BLITZ, 0),
                                             minRating: LichessArenaRatingObj = LichessArenaRatingObj(LichessPerfType.BLITZ, 0), 
                                             minRatedGames: LichessMinRatedGames = LichessMinRatedGames(0), onlyTitled: Boolean = false, teamMember: String = "",
                                             @upickle.implicits.key("private") privat: Boolean = false, position: LichessArenaPosition = LichessCustomPosition("", ""), 
                                             schedule: LichessSchedule = LichessSchedule("", ""), teamBattle: LichessTeamBattle = LichessTeamBattle(Array(""), 0), winner: LichessWinner = LichessWinner("", "")) extends LichessTournamentListEntry:
    def getStartTime: DateTime =
      DateTime(startsAt.toLong)
    def getName: String = fullName
    def getId: String = id
  object LichessArenaTournamentListEntry:
    implicit val latl: ReadWriter[LichessArenaTournamentListEntry] = macroRW
  
  case class LichessClock(limit: Integer, increment: Integer) derives ReadWriter

  case class LichessMinRatedGames(nb: Integer = 0) derives ReadWriter
  
  case class LichessSchedule(freq: String, speed: String) derives ReadWriter

  case class LichessStats(games: Integer, whiteWins: Integer, blackWins: Integer, draws: Integer, byes: Integer, absences: Integer, averageRating: Integer)
  object LichessStats:
    implicit val lst: ReadWriter[LichessStats] = macroRW

  case class LichessSwissTournamentListEntry(id: String, createdBy: String, startsAt: String, name: String, clock: LichessClock, variant: String,
                                             round: Integer, nbRounds: Integer, nbPlayers: Integer, nbOngoing: Integer, status: String, stats: LichessStats = LichessStats(0,0,0,0,0,0,0), 
                                             rated: Boolean, verdicts: LichessVerdicts) extends LichessTournamentListEntry:
    def getStartTime: DateTime =
      val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      formatter.parseDateTime(startsAt)
    def getName: String = name
    def getId: String = id
  object LichessSwissTournamentListEntry:
    implicit val lstl: ReadWriter[LichessSwissTournamentListEntry] = macroRW
  
  case class LichessTeamBattle(teams: Array[String], nbLeaders: Integer)
  object LichessTeamBattle:
    implicit val ltb: ReadWriter[LichessTeamBattle] = macroRW

  trait LichessTournamentListEntry:
    def getStartTime: DateTime
    def getName: String
    def getId: String
  
  case class LichessVariant(key: String, name: String, short: String)
  object LichessVariant:
    implicit val lat: ReadWriter[LichessVariant] = macroRW

  case class LichessVerdictObject(condition: String, verdict: String)
  object LichessVerdictObject:
    implicit val lvo: ReadWriter[LichessVerdictObject] = macroRW

  case class LichessVerdicts(accepted: Boolean, list: Array[LichessVerdictObject])
  object LichessVerdicts:
    implicit val lvd: ReadWriter[LichessVerdicts] = macroRW

  case class LichessWinner(id: String, name: String = "") derives ReadWriter
