package de.qno.tournamentadmin
package lichess

import scala.util.{Failure, Success, Try}
import scala.collection.mutable.ListBuffer
import org.joda.time.LocalDate
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*
import lichess.LichessInternalDataTypes.*

//TODO: Move getters that do not need auth to object
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

  private def getArenaInfos(id: String): Either[String, String] =
    val composedUrl: String = s"https://lichess.org/api/tournament/$id"
    basicRequest
      .auth.bearer(ltoken)
      .get(uri"$composedUrl")
      .response(asString)
      .send(DefaultSyncBackend())
      .body
  
  private def getArenaInfoString(id: String, teamId: String): String =  
    getArenaInfos(id) match
      case Right(s: String) => 
        val t: Try[String] =
          Try(read[ArenaTeamInfo](s).printString(teamId))
          
        t match
          case Success(st) => st
          case _ => read[ArenaSingleInfo](s).printString()
      case Left(s: String) => s

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

object LichessApi:
  /**
   * Filter predicate. Tests if the "startsAt" parameter of a JSON equals tDate.
   *
   * @param x a ujson.Value from a collection
   * @return true if "startsAt" equals tDate, false otherwise.
   */
  private def hasThisDate(fDate: LocalDate)(x: TournamentListEntry): Boolean =
    LocalDate(x.getStartTime).equals(fDate)

  /**
   * Mapping function LichessArenaTournamentList -> String
   *
   * @param x the LichessArenaTournamentList to map describing a tournament
   * @return a String containing starting time, name, and link to tournament
   */
  private def announceString(x: TournamentListEntry): String =
    val time: String = LocalDate(x.getStartTime).toString("HH:mm")
    val fullname = x.getName
    val idt = x.getId
    s"$time Uhr: $fullname https://lichess.org/tournament/$idt\n"

  /**
   * Mapping function. Maps a LichessTournamentListEntry to a String containing it's Id followed by a line break.
   * @param x the LichessTournamentListEntry to map
   * @return the mapped String
   */
  private def idString(x: TournamentListEntry): String =
    x.getId

  /**
   * Gets all description items to a teamId
   * 
   * @param teamId the Id of the team
   * @return a Team object with all information
   */
  def fetchTeam(teamId: String): Team =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId"
    basicRequest
      .get(uri"$composedUrl")
      .response(asJson[Team].getRight)
      .send(DefaultSyncBackend())
      .body
  
  /**
   * Get a List of Lichess Arena tournaments of a team at a date 
   *
   * @param teamId the ID of a team
   * @param tDate the Date of the requested tournaments            
   * @return a List of LichessArenaTournamentListEntry
   */
  private def fetchTeamsArenas(tDate: LocalDate, teamId: String): List[ArenaTournamentListEntry] =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/arena"
    val arenaList = ListBuffer[ArenaTournamentListEntry]()
    val stringIterator: Iterator[String] = basicRequest
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
      .linesIterator
    for line <- stringIterator do 
      arenaList.append(read[ArenaTournamentListEntry](line))  
    arenaList.toList.filter(hasThisDate(tDate))
    

  /**
   * Gets a List of a team's LichessArenaTournamentListEntry at a date and maps it to 
   * a List of their Ids.
   * 
   * @param tDate the Date to filter
   * @param teamId the team that owns the arenas
   * @return a List of Id Strings
   */
  private def getArenasIds(tDate: LocalDate, teamId: String): List[String] =
    val lichessArenasList = fetchTeamsArenas(tDate, teamId)
    val lichessArenaIdsList = lichessArenasList.map(idString)
    lichessArenaIdsList

  private def getArenasAnnouncements(tlist: List[ArenaTournamentListEntry]): List[String] =
    tlist.map(announceString)

  /**
   * Fetches the result and info of an arena tournament.
   *
   * Parameter teamId is necessary only in case of a team tournament. If it is sure that the arena is a single player tournament, the parameter can be omitted.
   *
   * @param id The Id of the Arena
   * @param teamId The Id of the team in question. Can be omitted in single player arenas and defaults to empty string
   * @return A String containing some Info and the first three placed players. In the case of a team tournament, the place of Team teamId and its 3 best players is added.
   */
  private def fetchArenaResult(id: String, teamId: String = ""): String =
    val composedUrl: String = s"https://lichess.org/api/tournament/$id"
    val stri: String = basicRequest
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
    val t: Try[String] =
      Try(read[ArenaTeamInfo](stri).printString(teamId))
    t match
      case Success(va) => va
      case Failure(_) => read[ArenaSingleInfo](stri).printString()
      
  private def getArenaInfos(ids: List[String], teamId: String = ""): List[String] =
    for
      id <- ids
    yield 
      fetchArenaResult(id, teamId)
        
  /**
   * Get a List of Lichess Swiss tournaments of a team at a date 
   *
   * @param teamId the ID of a team
   * @param tDate the Date of the requested tournaments            
   * @return a List of LichessArenaTournamentListEntry
   */
  private def fetchTeamsSwiss(tDate: LocalDate, teamId: String): List[SwissTournamentListEntry] =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/swiss"
    val swissList = ListBuffer[SwissTournamentListEntry]()
    val stringIterator: Iterator[String] = basicRequest
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
      .linesIterator
    for line <- stringIterator do
      swissList.append(read[SwissTournamentListEntry](line))
    swissList.toList.filter(hasThisDate(tDate))

  /**
   * Gets a List of a team's LichessSwissTournamentListEntry at a date and maps it to 
   * a List of their Ids.
   *
   * @param tDate the Date to filter
   * @param teamId the team that owns the arenas
   * @return a List of Id Strings
   */
  private def getSwissIds(tDate: LocalDate, teamId: String): List[String] =
    fetchTeamsSwiss(tDate, teamId).map(idString)

  private def getSwissAnnouncements(tlist: List[SwissTournamentListEntry]): List[String] =
    tlist.map(announceString)

  /**
   * Fetches the result of a single swiss tournament.
   *
   * @param id the Id of the tournament
   * @return A String containing some Info and the first three placed players.
   */
  private def fetchSwissResult(id: String): String =
    val composedUrl: String = s"https://lichess.org/api/swiss/$id"
    val info: SwissInfo = basicRequest
      .get(uri"$composedUrl")
      .response(asJson[SwissInfo].getRight)
      .send(DefaultSyncBackend())
      .body
    val composedUrl2: String = s"https://lichess.org/api/swiss/$id/results"
    val result: Array[SwissResult] = basicRequest
      .get(uri"$composedUrl2")
      .response(asJson[Array[SwissResult]].getRight)
      .send(DefaultSyncBackend())
      .body
    val buffer = StringBuilder(info.printString())
    for i <- 0 until math.min(3, result.length) do
      buffer.append(result(i).printString())
    buffer.toString()

  private def getSwissInfos(ids: List[String], teamId: String = ""): List[String] =
    for
      id <- ids
    yield
      fetchSwissResult(id)

  def getTournamentAnnouncements(tDate: LocalDate = LocalDate(), teamId: String): String =
    (getArenasAnnouncements(fetchTeamsArenas(tDate, teamId)) ::: getSwissAnnouncements(fetchTeamsSwiss(tDate, teamId))).sorted.foldLeft("")(_ + '\n' + _)

  def getTournamentInfos(tDate: LocalDate = LocalDate(), teamId: String): List[String] =
    val arenaIdList = getArenasIds(tDate, teamId)
    val swissIdList = getSwissIds(tDate, teamId)
    getArenaInfos(arenaIdList, teamId) ++ getSwissInfos(swissIdList, teamId)
