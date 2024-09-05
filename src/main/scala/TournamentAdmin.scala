package de.qno.tournamentadmin

import TournamentAdmin.TournamentType
import upickle.default.*
import sttp.client4.*
import org.joda.time.*

/**
 * Main class of package. Provides static properties, types, and methods
 * 
 * Uses file src/main/resources where access tokens are saved. Do not distribute this file!
 * Change IDs to your need. No other changes should be necessary.
 * Of course you have to edit series.json and instances.json to fit to your tournaments.
 */
object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  
  enum TournamentType derives ReadWriter:
    case LichessSwiss, LichessArena

  /**
   * Gets a list of team's Lichess Arenas on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessArenas(tDate: LocalDate = LocalDate()): String =
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
      s"$time Uhr: $fullname https://lichess.org/tournaments/$idt\n"
      
    LichessApi.getArena().map(ujson.read(_))
      .filter(p(_))
      .map(m(_))
      .foldLeft("")(_ + _)

  /**
   * Gets a list of team's Lichess Swiss tournaments on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessSwiss(tDate: LocalDate = LocalDate()): String =
    /**
     * Filter predicate. Tests if the "startsAt" parameter of a JSON equals tDate.
     * @param x a ujson.Value from a collection
     * @return true if "startsAt" equals tDate, false otherwise.
     */
    def p(x: ujson.Value): Boolean =
      org.joda.time.DateTime.parse(x("startsAt").str).toDateTime(DateTimeZone.getDefault).equals(tDate)
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
      s"$time Uhr: $fullname https://lichess.org/tournaments/$idt\n"
      
    LichessApi.getSwiss().map(ujson.read(_))
      .filter(p(_))
      .map(m(_))
      .foldLeft("")(_ + _)

  /**
   * Construct and send a message announcing todays tournaments to
   * - the Lichess team
   * - the Bluesky account
   */
  //TODO: Twitter
  @main
  def sendMessage(): Unit =
    val startText = "Heutige Turniere:\n"
    
    val text = startText ++ getLichessArenas() ++ getLichessSwiss()

    if text.nonEmpty then
      LichessApi.sendMessage(text)
      print(text)

