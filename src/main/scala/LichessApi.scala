package de.qno.tournamentadmin

import sttp.client4.*

case class LichessApi(private val token: String):
  /**
   * Create a Lichess Arena.
   * All Parameters are Strings.
   * @param name Name of Tournament
   * @param time Clock initial time in MINUTES
   * @param increment Clock increment in seconds
   * @param minutes How long tournament lasts, in minutes
   * @param startDate Start Date as Timestamp in milliseconds
   * @param description Tournament description
   * @param team Only players of team allowed; defaults to TournamentEntry.teamID
   * @return ID of created tournament
   */
  def createArena(name: String, time: String, increment: String, minutes: String, startDate: String, description: String, team: String = TournamentAdmin.teamID): String =
    val creationMap = Map(
      "name" -> name,
      "clockTime" -> time,
      "clockIncrement" -> increment,
      "minutes" -> minutes,
      "startDate" -> startDate,
      "description" -> description,
      "conditions.teamMember.teamId" -> team
    )
    ujson.read(basicRequest
      .auth.bearer(token)
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
      "additionalConds" -> TournamentAdmin.teamID,
      "conditions.maxRating.rating" -> maxRating,
      "conditions.playYourGames" -> "true"
    )
    val composedUrl: String = s"https://lichess.org/api/swiss/new/${TournamentAdmin.teamID}"
    ujson.read(basicRequest
      .auth.bearer(token)
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
  def getArena(team: String = TournamentAdmin.teamID): Iterator[String] =
    val composedUrl: String = s"https://lichess.org/api/team/$team/arena"
    basicRequest
      .auth.bearer(token)
      .get(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body.linesIterator

  /**
   * Get an Iterator over a nlJSON list of Lichess Swiss tournaments of a team 
   * @param team the ID of a team, defaults to DSB
   * @return an Iterator[String] containing one JSON tournament description per line.
   */
  def getSwiss(team: String = TournamentAdmin.teamID): Iterator[String] =
    val composedUrl: String = s"https://lichess.org/api/team/$team/swiss"
    basicRequest
      .auth.bearer(token)
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
    val composedUrl = s"https://lichess.org/team/${TournamentAdmin.teamID}/pm-all"
    val resp = ujson.read(basicRequest
      .auth.bearer(token)
      .body(Map("message" -> text))
      .post(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body)
    resp("ok").bool