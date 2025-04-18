package de.qno.tournamentadmin
package lichess

import org.joda.time.*
import org.joda.time.format.*
import upickle.default.*

object LichessInternalDataTypes:
  //Types
  type Podium = List[LichessArenaPlayerPerformance]
  
  //Enums
  enum System(system: String) derives ReadWriter:
    case ARENA extends System("arena")
    case SWISS extends System("swiss")

  enum PerfType(perf: String) derives ReadWriter:
    case ULTRABULLET extends PerfType("ultraBullet")
    case BULLET extends PerfType("bullet")
    case BLITZ extends PerfType("blitz")
    case RAPID extends PerfType("rapid")
    case CLASSICAL extends PerfType("classical")
    case CORRESPONDENCE extends PerfType("correspondence")
    case CHESS960 extends PerfType("chess960")
    case CRAZYHOUSE extends PerfType("crazyhouse")
    case ANTICHESS extends PerfType("antichess")
    case ATOMIC extends PerfType("atomic")
    case HORDE extends PerfType("horde")
    case KINGOFTHEHILL extends PerfType("kingOfTheHill")
    case RACINGKINGS extends PerfType("racingKings")
    case THREECHECK extends PerfType("threeCheck")

  enum Status(status: Integer) derives ReadWriter:
    case CREATED extends Status(10)
    case STARTED extends Status(20)
    case FINISHED extends Status(30)

  enum VariantKey(variant: String) derives ReadWriter:
    case standard extends VariantKey("standard")
    case CHESS960 extends VariantKey("chess960")
    case CRAZYHOUSE extends VariantKey("crazyhouse")
    case ANTICHESS extends VariantKey("antichess")
    case ATOMIC extends VariantKey("atomic")
    case HORDE extends VariantKey("horde")
    case KINGOFTHEHILL extends VariantKey("kingOfTheHill")
    case RACINGKINGS extends VariantKey("racingKings")
    case THREECHECK extends VariantKey("threeCheck")
    case FROMPOSITION extends VariantKey("fromPosition")

  //Info related classes
  abstract class ArenaInfo(id: String, fullName: String, rated: Boolean, berserkable: Boolean, clock: Clock,
                           minutes: Int, createdBy: String, system: String, secondsToStart: Int, secondsToFinish: Int, isFinished: Boolean, isRecentlyFinished: Boolean,
                           pairingsClosed: Boolean, startsAt: String, nbPlayers: Int, verdicts: Verdicts, quote: LichessQuote, hasMaxRating: Boolean,
                           maxRating: LichessArenaRatingObj, minRating: LichessArenaRatingObj, minRatedGames: MinRatedGames, botsAllowed: Boolean, minAccountAgeInDays: Int,
                           perf: LichessArenaPerfA, schedule: LichessSchedule, variant: VariantKey, duels: List[LichessDuels], standing: LichessStanding)

  case class ArenaSingleInfo(id: String, fullName: String, rated: Boolean = false, berserkable: Boolean = false, clock: Clock,
                             minutes: Int = 0, createdBy: String = "", system: String = "", secondsToStart: Int = 0, secondsToFinish: Int = 0, isFinished: Boolean = false, isRecentlyFinished: Boolean = false,
                             pairingsClosed: Boolean = false, startsAt: String = "", nbPlayers: Int, verdicts: Verdicts, quote: LichessQuote = LichessQuote("",""), hasMaxRating: Boolean = false,
                             maxRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.CLASSICAL, 10000), minRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.CLASSICAL,0), minRatedGames: MinRatedGames = MinRatedGames(0), botsAllowed: Boolean = false, minAccountAgeInDays: Int = 0,
                             perf: LichessArenaPerfA, schedule: LichessSchedule = LichessSchedule("", ""), variant: VariantKey, duels: List[LichessDuels], standing: LichessStanding,
                             spotlight: LichessSpotlight = LichessSpotlight(""), onlyTitled: Boolean = false, allowList: Array[String] = Array()) extends ArenaInfo(
    id, fullName, rated, berserkable, clock, minutes, createdBy, system, secondsToStart, secondsToFinish, isFinished, isRecentlyFinished, pairingsClosed, startsAt, nbPlayers, verdicts, quote, hasMaxRating,
    maxRating, minRating, minRatedGames, botsAllowed, minAccountAgeInDays, perf, schedule, variant, duels, standing) derives ReadWriter:
    def printString(): String =
      val fmt = new DateTimeFormatterBuilder()
        .appendDayOfMonth(2)
        .appendLiteral(". ")
        .appendMonthOfYearText()
        .appendLiteral(' ')
        .appendYear(4,4)
        .appendLiteral(' ')
        .appendHourOfDay(2)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .toFormatter()
      s"${fullName} ${fmt.print(DateTime(startsAt))}\n${standing}"

  case class ArenaTeamInfo(id: String, fullName: String, rated: Boolean = false, berserkable: Boolean = false, clock: Clock,
                           minutes: Int = 0, createdBy: String = "", system: String = "", secondsToStart: Int = 0, secondsToFinish: Int = 0, isFinished: Boolean = false, isRecentlyFinished: Boolean = false,
                           pairingsClosed: Boolean = false, startsAt: String = "", nbPlayers: Int, verdicts: Verdicts, quote: LichessQuote = LichessQuote("",""), hasMaxRating: Boolean = false,
                           maxRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.CLASSICAL,10000), minRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.CLASSICAL,0), minRatedGames: MinRatedGames = MinRatedGames(0), botsAllowed: Boolean = false, minAccountAgeInDays: Int = 0,
                           perf: LichessArenaPerfA, schedule: LichessSchedule = LichessSchedule("",""), variant: VariantKey, duels: List[LichessDuels], standing: LichessStanding,
                           podium: Podium, teamStanding: List[LichessArenaTeamPerformance], teamBattle: ujson.Value) extends ArenaInfo(
    id, fullName, rated, berserkable, clock, minutes, createdBy, system, secondsToStart, secondsToFinish, isFinished, isRecentlyFinished, pairingsClosed, startsAt, nbPlayers, verdicts, quote, hasMaxRating,
    maxRating, minRating, minRatedGames, botsAllowed, minAccountAgeInDays, perf, schedule, variant, duels, standing) derives ReadWriter:
    def printString(team: String): String =
      val fmt = new DateTimeFormatterBuilder()
        .appendDayOfMonth(2)
        .appendLiteral(". ")
        .appendMonthOfYearText()
        .appendLiteral(' ')
        .appendYear(4,4)
        .appendLiteral(' ')
        .appendHourOfDay(2)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .toFormatter()
      val buffer = StringBuilder(s"${fullName} ${fmt.print(DateTime(startsAt))}\n")
      for i <- 0 until math.min(3, teamStanding.length) do
        buffer.append(teamStanding(i))
      val ownTeam = teamStanding.filter(_.id == team).head
      buffer.append("\n" + ownTeam.teamPerformance())
      buffer.toString
      
  case class SwissInfo(id: String, createdBy: String, startsAt: String, name: String, clock: Clock, variant: VariantKey, round: Int, nbRounds: Int, nbOngoing: Int, status: Status, stats: Stats, rated: Boolean, verdicts: Verdicts) 
    derives ReadWriter:
    def printString(): String =
      val fmt = new DateTimeFormatterBuilder()
        .appendDayOfMonth(2)
        .appendLiteral(". ")
        .appendMonthOfYearText()
        .appendLiteral(' ')
        .appendYear(4, 4)
        .appendLiteral(' ')
        .appendHourOfDay(2)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .toFormatter()
      s"${name} ${fmt.print(DateTime(startsAt))}\n"
      
  case class SwissResult(absent: Boolean = false, rank: Int, points: Int, tieBreak: Int, rating: Int, username: Int, title: String = "", performance: Int) derives ReadWriter:
    def printString(): String =
      s"${rank}. ${title} ${username} ${points}\n"
        
  //case classes
  case class LichessArenaPerf(key: String, name: String, position: Integer, icon: String) derives ReadWriter

  case class LichessArenaPerfA(key: String, name: String, icon: String) derives ReadWriter

  case class LichessArenaPlayerPerformance(name: String, rank: Int, title: String = "", patron: Boolean = false, flair: String = "", rating: Int = 0, score: Int, 
                                           sheet: LichessArenaSheet = LichessArenaSheet("", false), nb: LichessNB = LichessNB(0,0,0), performance: Int = 0, team: String = "") derives ReadWriter:
    override def toString: String =
      s"${rank}. ${name} ${score}\n"

  sealed trait LichessArenaPosition derives ReadWriter
  case class LichessThematic(eco: String, name: String, fen: String, url: String) extends LichessArenaPosition
  case class LichessCustomPosition(name: String, fen: String) extends LichessArenaPosition

  case class LichessArenaRatingObj(perf: PerfType, rating: Integer)
  object LichessArenaRatingObj:
    implicit val laro: ReadWriter[LichessArenaRatingObj] = macroRW

  case class LichessArenaSheet(scores: String, fire: Boolean = false) derives ReadWriter
  
  case class LichessTeamPlayerId(name: String, flair: String = "", id: String) derives ReadWriter:
    override def toString(): String =
      name
  
  case class LichessTeamPlayers(user: LichessTeamPlayerId, score: Int) derives ReadWriter:
    override def toString: String =
      s"${user.toString} ${score} Punkte \n"

  case class LichessArenaTeamPerformance(rank: Int, id: String, score: Int, players: List[LichessTeamPlayers]) derives ReadWriter:
    override def toString(): String =
      s"${rank}. ${LichessApi.fetchTeam(id).name} ${score}\n"
    def teamPerformance(): String =
      val builder = StringBuilder()
      builder.append(s"Team ${id} belegte Platz ${rank} mit ${score} Punkten.\n")
      builder.append("Die besten Spieler waren: \n")
      for i <- 0 until math.min(3, players.length) do
        builder.append(players(i).toString())
      builder.toString

  case class ArenaTournamentListEntry(id: String, createdBy: String, system: String, minutes: Integer, clock: Clock, rated: Boolean, fullName: String,
                                      nbPlayers: Integer, variant: Variant, startsAt: Long, finishesAt: Long, status: Integer, perf: LichessArenaPerf, secondsToStart: Integer = 0,
                                      hasMaxRating: Boolean = false, maxRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.BLITZ, 0),
                                      minRating: LichessArenaRatingObj = LichessArenaRatingObj(PerfType.BLITZ, 0),
                                      minRatedGames: MinRatedGames = MinRatedGames(0), onlyTitled: Boolean = false, teamMember: String = "",
                                      @upickle.implicits.key("private") privat: Boolean = false, position: LichessArenaPosition = LichessCustomPosition("", ""),
                                      schedule: LichessSchedule = LichessSchedule("", ""), teamBattle: LichessTeamBattle = LichessTeamBattle(List[ujson.Value](), 0), winner: Winner = Winner("", "")) extends LichessTournamentListEntry:
    def getStartTime: DateTime =
      DateTime(startsAt)
    def getName: String = fullName
    def getId: String = id
  object ArenaTournamentListEntry:
    implicit val latl: ReadWriter[ArenaTournamentListEntry] = macroRW
  
  case class Clock(limit: Integer, increment: Integer) derives ReadWriter

  case class LichessDuels(id: String, p: List[LichessDuelInterna]) derives ReadWriter
  
  case class LichessDuelInterna(n: String, r: Int, k: Int) derives ReadWriter
  
  case class MinRatedGames(nb: Int = 0) derives ReadWriter
  
  case class LichessNB(game: Int, berserk: Int, win: Int) derives ReadWriter
  
  case class LichessQuote(text: String = "", author: String = "") derives ReadWriter
  
  case class LichessSchedule(freq: String, speed: String) derives ReadWriter

  case class LichessSpotlight(headline: String) derives ReadWriter
  
  case class LichessStanding(page: Int, players: Array[LichessArenaPlayerPerformance]) derives ReadWriter:
    override def toString: String =
      val buffer = new StringBuilder()
      for i <- 0 until math.min(3, players.length) do
        buffer.append(players(i).toString)
      buffer.toString  
  
  case class Stats(games: Integer, whiteWins: Integer, blackWins: Integer, draws: Integer, byes: Integer, absences: Integer, averageRating: Integer) derives ReadWriter

  case class SwissTournamentListEntry(id: String, createdBy: String, startsAt: String, name: String, clock: Clock, variant: String,
                                      round: Integer, nbRounds: Integer, nbPlayers: Integer, nbOngoing: Integer, status: String, stats: Stats = Stats(0,0,0,0,0,0,0),
                                      rated: Boolean, verdicts: Verdicts = Verdicts(false, List[Verdict]())) extends LichessTournamentListEntry derives ReadWriter:
    def getStartTime: DateTime =
      val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      formatter.parseDateTime(startsAt)
    def getName: String = name
    def getId: String = id
  
  case class LichessTeamBattle(teams: List[ujson.Value], nbLeaders: Integer) derives ReadWriter
    
  trait LichessTournamentListEntry:
    def getStartTime: DateTime
    def getName: String
    def getId: String
  
  //TODO: ReadWriter uses VariantKey
  case class Variant(key: String = "", name: String = "", short: String = "")
  object Variant:
    implicit val lat: ReadWriter[Variant] = macroRW

  case class Verdict(condition: String, verdict: String)
  object Verdict:
    implicit val lvo: ReadWriter[Verdict] = macroRW

  case class Verdicts(accepted: Boolean, list: List[Verdict])
  object Verdicts:
    implicit val lvd: ReadWriter[Verdicts] = macroRW

  case class Winner(id: String, name: String = "") derives ReadWriter
  
  case class Team(id: String, name: String, description: String = "", flair: String = "", leaders: List[LightUser], nbMembers: Int, open: Boolean, joined: Boolean, requested: Boolean) derives ReadWriter
  
  case class LightUser(id: String, name: String, title: String = "", patron: Boolean = false) derives ReadWriter
