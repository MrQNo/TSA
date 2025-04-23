package de.qno.tournamentadmin
package lichess

import de.qno.tournamentadmin.lichess.LichessInternalDataTypes.SwissResult
import org.joda.time.*
import org.joda.time.format.*
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*

object LichessInternalDataTypes:
  //Types
  private type Podium = List[ArenaPlayerPerformance]
  
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
    case created extends Status(10)
    case started extends Status(20)
    case finished extends Status(30)

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
                           pairingsClosed: Boolean, startsAt: String, nbPlayers: Int, verdicts: Verdicts, quote: Quote, hasMaxRating: Boolean,
                           maxRating: ArenaRatingObj, minRating: ArenaRatingObj, minRatedGames: MinRatedGames, botsAllowed: Boolean, minAccountAgeInDays: Int,
                           perf: ArenaPerfA, schedule: Schedule, variant: VariantKey, duels: List[Duels], standing: Standing)

  case class ArenaSingleInfo(id: String, fullName: String, rated: Boolean = false, berserkable: Boolean = false, clock: Clock,
                             minutes: Int = 0, createdBy: String = "", system: String = "", secondsToStart: Int = 0, secondsToFinish: Int = 0, isFinished: Boolean = false, isRecentlyFinished: Boolean = false,
                             pairingsClosed: Boolean = false, startsAt: String = "", nbPlayers: Int, verdicts: Verdicts, quote: Quote = Quote("",""), hasMaxRating: Boolean = false,
                             maxRating: ArenaRatingObj = ArenaRatingObj(PerfType.CLASSICAL, 10000), minRating: ArenaRatingObj = ArenaRatingObj(PerfType.CLASSICAL,0), minRatedGames: MinRatedGames = MinRatedGames(0), botsAllowed: Boolean = false, minAccountAgeInDays: Int = 0,
                             perf: ArenaPerfA, schedule: Schedule = Schedule("", ""), variant: VariantKey, duels: List[Duels], standing: Standing,
                             spotlight: Spotlight = Spotlight(""), onlyTitled: Boolean = false, allowList: Array[String] = Array()) extends ArenaInfo(
    id, fullName, rated, berserkable, clock, minutes, createdBy, system, secondsToStart, secondsToFinish, isFinished, isRecentlyFinished, pairingsClosed, startsAt, nbPlayers, verdicts, quote, hasMaxRating,
    maxRating, minRating, minRatedGames, botsAllowed, minAccountAgeInDays, perf, schedule, variant, duels, standing) derives ReadWriter:
    def printString: String =
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
        .toFormatter
      s"$fullName ${fmt.print(DateTime(startsAt))}\n$standing"

  case class ArenaTeamInfo(id: String, fullName: String, rated: Boolean = false, berserkable: Boolean = false, clock: Clock,
                           minutes: Int = 0, createdBy: String = "", system: String = "", secondsToStart: Int = 0, secondsToFinish: Int = 0, isFinished: Boolean = false, isRecentlyFinished: Boolean = false,
                           pairingsClosed: Boolean = false, startsAt: String = "", nbPlayers: Int, verdicts: Verdicts, quote: Quote = Quote("",""), hasMaxRating: Boolean = false,
                           maxRating: ArenaRatingObj = ArenaRatingObj(PerfType.CLASSICAL,10000), minRating: ArenaRatingObj = ArenaRatingObj(PerfType.CLASSICAL,0), minRatedGames: MinRatedGames = MinRatedGames(0), botsAllowed: Boolean = false, minAccountAgeInDays: Int = 0,
                           perf: ArenaPerfA, schedule: Schedule = Schedule("",""), variant: VariantKey, duels: List[Duels], standing: Standing,
                           podium: Podium, teamStanding: List[ArenaTeamPerformance], teamBattle: ujson.Value) extends ArenaInfo(
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
        .toFormatter
      val buffer = StringBuilder(s"$fullName ${fmt.print(DateTime(startsAt))}\n")
      for stand <- teamStanding.take(3) do
        buffer.append(stand.printString)
      val ownTeam = teamStanding.filter(_.id == team).head
      buffer.append("\n" + ownTeam.teamPerformance())
      buffer.toString
      
  case class SwissInfo(id: String, createdBy: String, startsAt: String, name: String, clock: Clock, variant: VariantKey, round: Int, nbRounds: Int, nbOngoing: Int, status: Status, stats: Stats, rated: Boolean, verdicts: Verdicts) 
    derives ReadWriter:
    def printString: String =
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
        .toFormatter
      val fmt2 = ISODateTimeFormat.dateTimeNoMillis()
      s"$name ${fmt.print(fmt2.parseDateTime(startsAt))}\n"
      
  case class SwissResult(absent: Boolean = false, rank: Int, points: Int, tieBreak: Int, rating: Int, username: String, title: String = "", performance: Int = 0) derives ReadWriter:
    def printString: String =
      s"$rank. $title $username $points\n"
        
  //other case classes
  // all Tournaments
  trait TournamentListEntry:
    def getStartTime: DateTime
    def getName: String
    def getId: String

  case class Standing(page: Int, players: Array[ArenaPlayerPerformance]) derives ReadWriter:
    override def toString: String =
      val buffer = new StringBuilder()
      for player <- players.take(3) do
        buffer.append(player.printString)
      buffer.toString

  // Arena related
  case class ArenaPerf(key: String, name: String, position: Integer, icon: String) derives ReadWriter

  case class ArenaPerfA(key: String, name: String, icon: String) derives ReadWriter

  case class ArenaPlayerPerformance(name: String, rank: Int, title: String = "", patron: Boolean = false, flair: String = "", rating: Int = 0, score: Int,
                                    sheet: ArenaSheet = ArenaSheet("", false), nb: Nb = Nb(0,0,0), performance: Int = 0, team: String = "") derives ReadWriter:
    def printString: String =
      s"$rank. $name $score\n"

  sealed trait ArenaPosition derives ReadWriter
  private case class ArenaPositionThematic(eco: String, name: String, fen: String, url: String) extends ArenaPosition
  private case class ArenaPositionCustom(name: String, fen: String) extends ArenaPosition

  case class ArenaRatingObj(perf: PerfType, rating: Integer)
  object ArenaRatingObj:
    implicit val laro: ReadWriter[ArenaRatingObj] = macroRW

  case class ArenaSheet(scores: String, fire: Boolean = false) derives ReadWriter

  case class ArenaTournamentListEntry(id: String, createdBy: String, system: String, minutes: Integer, clock: Clock, rated: Boolean, fullName: String,
                                      nbPlayers: Integer, variant: Variant, startsAt: Long, finishesAt: Long, status: Integer, perf: ArenaPerf, secondsToStart: Integer = 0,
                                      hasMaxRating: Boolean = false, maxRating: ArenaRatingObj = ArenaRatingObj(PerfType.BLITZ, 0),
                                      minRating: ArenaRatingObj = ArenaRatingObj(PerfType.BLITZ, 0),
                                      minRatedGames: MinRatedGames = MinRatedGames(0), onlyTitled: Boolean = false, teamMember: String = "",
                                      @upickle.implicits.key("private") privat: Boolean = false, position: ArenaPosition = ArenaPositionCustom("", ""),
                                      schedule: Schedule = Schedule("", ""), teamBattle: TeamBattle = TeamBattle(List[ujson.Value](), 0), winner: Winner = Winner("", "")) extends TournamentListEntry:
    def getStartTime: DateTime =
      DateTime(startsAt)
    def getName: String = fullName
    def getId: String = id
  object ArenaTournamentListEntry:
    implicit val latl: ReadWriter[ArenaTournamentListEntry] = macroRW
  
  //Team Arena related
  case class Team(id: String, name: String, description: String = "", flair: String = "", leaders: List[LightUser] = List(), nbMembers: Int = 0, open: Boolean = false, joined: Boolean = false, requested: Boolean = false) derives ReadWriter

  case class ArenaTeamPerformance(rank: Int, id: String, score: Int, players: List[TeamPlayer])derives ReadWriter:
    def printString: String =
      s"$rank. ${de.qno.tournamentadmin.lichess.LichessApi.fetchTeam(id).name} $score\n"

    def teamPerformance(): String =
      val builder = StringBuilder()
      builder.append(s"Team $id belegte Platz $rank mit $score Punkten.\n")
      builder.append("Die besten Spieler waren: \n")
      for player <- players.take(3) do
        builder.append(player.printString)
      builder.toString

  case class TeamBattle(teams: List[ujson.Value], nbLeaders: Integer) derives ReadWriter

  
  //Swiss related
  case class SwissTournamentListEntry(id: String, createdBy: String, startsAt: String, name: String, clock: Clock, variant: String,
                                      round: Integer, nbRounds: Integer, nbPlayers: Integer, nbOngoing: Integer, status: String, stats: Stats = Stats(0,0,0,0,0,0,0),
                                      rated: Boolean, verdicts: Verdicts = Verdicts(false, List[Verdict]())) extends TournamentListEntry derives ReadWriter:
    def getStartTime: DateTime =
      val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      formatter.parseDateTime(startsAt)
    def getName: String = name
    def getId: String = id

  //Team related
  case class TeamPlayerId(name: String, flair: String = "", id: String) derives ReadWriter
  
  case class TeamPlayer(user: TeamPlayerId, score: Int) derives ReadWriter:
    def printString: String =
      s"${user.name} $score Punkte \n"
  
  //other other
  case class Clock(limit: Integer, increment: Integer) derives ReadWriter

  case class Duels(id: String, p: List[DuelInterna]) derives ReadWriter
  
  case class DuelInterna(n: String, r: Int, k: Int) derives ReadWriter

  case class LightUser(id: String, name: String, title: String = "", patron: Boolean = false)derives ReadWriter

  case class MinRatedGames(nb: Int = 0) derives ReadWriter
  
  case class Nb(game: Int, berserk: Int, win: Int) derives ReadWriter
  
  case class Quote(text: String = "", author: String = "") derives ReadWriter
  
  case class Schedule(freq: String, speed: String) derives ReadWriter

  case class Spotlight(headline: String) derives ReadWriter
  
  case class Stats(games: Integer, whiteWins: Integer, blackWins: Integer, draws: Integer, byes: Integer, absences: Integer, averageRating: Integer) derives ReadWriter

  //TODO: ReadWriter uses VariantKey
  case class Variant(key: String = "", name: String = "", short: String = "")
  object Variant:
    implicit val lat: ReadWriter[Variant] = macroRW

  case class Verdict(condition: String, verdict: String)
  object Verdict:
    implicit val lvo: ReadWriter[Verdict] = macroRW

  case class Verdicts(accepted: Boolean, list: List[Verdict]) derives ReadWriter
  
  case class Winner(id: String, name: String = "") derives ReadWriter
