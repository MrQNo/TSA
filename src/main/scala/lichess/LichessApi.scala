package de.qno.tournamentadmin
package lichess

import lichess.LichessInternalDataTypes.*

import org.joda.time.LocalDate
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*

case class LichessApi(teamId: String, ltoken: String):
  /**
   * Filter predicate. Tests if the "startsAt" parameter of a JSON equals tDate.
   *
   * @param x a ujson.Value from a collection
   * @return true if "startsAt" equals tDate, false otherwise.
   */
  def p(fDate: LocalDate)(x: LichessTournamentListEntry): Boolean =
    LocalDate(x.getStartTime).equals(fDate)

  /**
   * Mapping function LichessArenaTournamentList -> String
   *
   * @param x the LichessArenaTournamentList to map describing a tournament
   * @return a String containing starting time, name, and link to tournament
   */
  def m(x: LichessTournamentListEntry): String =
    val time: String = LocalDate(x.getStartTime).toString("HH:mm")
    val fullname = x.getName
    val idt = x.getId
    s"$time Uhr: $fullname https://lichess.org/tournament/$idt\n"

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
  def getArena: List[LichessArenaTournamentListEntry] =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/arena"
    basicRequest
      .auth.bearer(ltoken)
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body.linesIterator
      .map(read[LichessArenaTournamentListEntry](_)).toList

  /**
   * Gets a list of team's Lichess Arenas on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a List of String, one per tournament, with the starting time, the name, and the link to the tournament
   */
  def getLichessArenas(tDate: LocalDate): List[LichessArenaTournamentListEntry] =
    getArena.filter(p(tDate))

  def getLichessArenasDates(tlist: List[LichessArenaTournamentListEntry]): List[String] =
    tlist.map(m)

  /**
   * Get an Iterator over a nlJSON list of Lichess Swiss tournaments of a team 
   * @return an Iterator[String] containing one JSON tournament description per line.
   */
  def getSwiss: List[LichessSwissTournamentListEntry] =
    val composedUrl: String = s"https://lichess.org/api/team/$teamId/swiss"
    basicRequest
      .auth.bearer(ltoken)
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body.linesIterator
      .map(read[LichessSwissTournamentListEntry](_)).toList

  /**
   * Gets a list of team's Lichess Swiss tournaments on a day.
   * @param tDate the day the list relates to.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessSwiss(tDate: LocalDate): List[LichessSwissTournamentListEntry] =
    getSwiss.filter(p(tDate))

  def getLichessSwissDates(tlist: List[LichessSwissTournamentListEntry]): List[String] =
    tlist.map(m)
    
  def getTournamentAnnouncementDates(tDate: LocalDate = LocalDate()): List[String] =
    (getLichessArenasDates(getLichessArenas(tDate)) ::: getLichessSwissDates(getLichessSwiss(tDate))).sorted  

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
