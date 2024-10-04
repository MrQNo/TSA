package de.qno.tournamentadmin

import upickle.default.*
import org.joda.time.*
import LichessApi.*

/**
 * Main class of package. Provides static properties, types, and methods
 * 
 * Uses file src/main/resources where access tokens are saved. Do not distribute this file!
 * Change IDs to your need. No other changes should be necessary.
 * Of course you have to edit series.json and instances.json to fit to your tournaments.
 */
object TournamentAdmin:
  private val secrets = os.read.lines(os.pwd / "token.txt").iterator
  val teamID: String = secrets.next()
  private val lToken: String = secrets.next()
  private val bsUser = secrets.next()
  private val bsPassword: String = secrets.next()
  val xApiKey: String = secrets.next()
  private val xApiKeySecret: String = secrets.next()
  val xAccesToken: String = secrets.next()
  private val xAccessTokenSecret: String = secrets.next()
  
  enum TournamentType derives ReadWriter:
    case LichessSwiss, LichessArena

  /**
   * Gets a list of team's Lichess Arenas on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessArenas(session: LichessApi, tDate: LocalDate = LocalDate()): String =
    /**
     * Filter predicate. Tests if the "startsAt" parameter of a JSON equals tDate.
     * @param x a ujson.Value from a collection
     * @return true if "startsAt" equals tDate, false otherwise.
     */
    def p(x: ujson.Value): Boolean =
      LocalDate( DateTime( x("startsAt").num.toLong )).equals(tDate)
    /**
     * Mapping function ujson.Value -> String
     * @param x the ujson.Value to map describing a tournament
     * @return a String containing starting time, name, and link to tournament
     */
    def m(x: ujson.Value): String =
      val date = DateTime(x("startsAt").num.toLong)
      val time = LocalTime(date).toString("HH:mm")
      val fullname = x("fullName").str
      val idt = x("id").str
      s"$time Uhr: $fullname https://lichess.org/tournament/$idt\n"
      
    session.getArena().map(ujson.read(_))
      .filter(p)
      .map(m)
      .foldLeft("")(_ + _)

  /**
   * Gets a list of team's Lichess Swiss tournaments on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessSwiss(session: LichessApi, tDate: LocalDate = LocalDate()): String =
    /**
     * Filter predicate. Tests if the "startsAt" parameter of a JSON equals tDate.
     * @param x a ujson.Value from a collection
     * @return true if "startsAt" equals tDate, false otherwise.
     */
    def p(x: ujson.Value): Boolean =
      org.joda.time.DateTime.parse(x("startsAt").str)
        .toDateTime(DateTimeZone.getDefault) // adds TZ; necessary because server has UTC and i have not.
        .toLocalDate.equals(tDate) // removes time, date only
    /**
     * Mapping function ujson.Value -> String
     * @param x the ujson.Value to map describing a tournament
     * @return a String containing starting time, name, and link to tournament
     */
    def m(x: ujson.Value): String =
      val date = org.joda.time.DateTime.parse(x("startsAt").str).toDateTime(DateTimeZone.getDefault)
      val time = LocalTime(date).toString("HH:mm")
      val fullname = x("name").str
      val idt = x("id").str
      s"$time Uhr: $fullname https://lichess.org/swiss/$idt\n"
      
    session.getSwiss().toList.map(ujson.read(_))
      .filter(p)
      .map(m)
      .foldLeft("")(_ + _)

  private def makeTwitterKey: String =
    java.net.URLEncoder.encode(xApiKeySecret, java.nio.charset.Charset.defaultCharset()) + "&" + java.net.URLEncoder.encode(xAccessTokenSecret, java.nio.charset.Charset.defaultCharset())

  /**
   * Construct and send a message announcing todays tournaments to
   * - the Lichess team
   * - the Bluesky account
   */
  private def sendMessages(): Unit =
    val lichessSession: LichessApi = LichessApi(TournamentAdmin.lToken)
    TournamentInstance.create(lichessSession)

    val startText = "Heutige Turniere:\n"
    val newText = TournamentAdmin.getLichessArenas(lichessSession) ++ TournamentAdmin.getLichessSwiss(lichessSession)
    val text = startText ++ newText

    if newText.nonEmpty then
      lichessSession.sendMessage(text)
      val bsSession = Bluesky.createSession(TournamentAdmin.bsUser, TournamentAdmin.bsPassword)
      Bluesky.createRecord(bsSession, text)
      Twitter.createPost(text, TournamentAdmin.makeTwitterKey)
      print(text)

  @main
  def main(): Unit =
    sendMessages()