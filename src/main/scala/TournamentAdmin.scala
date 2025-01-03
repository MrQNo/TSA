package de.qno.tournamentadmin

import scala.util.*
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
  private val secrets = os.read.lines(os.pwd / "twitter.token").iterator

  private val lichessSecretsPath = os.pwd / "lichess.token"
  private val lichessSecrets = os.read.lines(lichessSecretsPath).iterator
  private val lichessSession = LichessApi(lichessSecrets.next(), lichessSecrets.next())

  private val blueskySecretsPath = os.pwd / "bluesky.token"
  private val blueskySecrets = os.read.lines(blueskySecretsPath).iterator
  private val blueskyCreds: Try[BlueskyCredentials] = Try(BlueskyCredentials(blueskySecrets.next(), blueskySecrets.next()))

  private val twitterSecretsPath = os.pwd / "twitter.token"
  private val twitterSecrets = os.read.lines(twitterSecretsPath).iterator
  private val twitterCreds: Try[TwitterCredentials] = Try(TwitterCredentials(twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next()))

  enum TournamentType derives ReadWriter:
    case LichessSwiss, LichessArena

  /**
   * Gets a list of team's Lichess Arenas on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a List of String, one per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessArenas(session: LichessApi, tDate: LocalDate = LocalDate()): List[LichessArenaTournamentListEntry] =
    /**
     * Filter predicate. Tests if the "startsAt" parameter of a LichessArenaTournamentList equals tDate.
     * @param x a LichessArenaTournamentList from a collection
     * @return true if "startsAt" equals tDate, false otherwise.
     */
    def p(x: LichessArenaTournamentListEntry): Boolean =
      LocalDate( DateTime( x.startsAt.toLong )).equals(tDate)

    session.getArena().created.toList.filter(p)

  private def getLichessArenasDates(tlist: List[LichessArenaTournamentListEntry]): List[String] =
    /**
     * Mapping function LichessArenaTournamentList -> String
     * @param x the LichessArenaTournamentList to map describing a tournament
     * @return a String containing starting time, name, and link to tournament
     */
    def m(x: LichessArenaTournamentListEntry): String =
      val date = DateTime(x.startsAt.toLong)
      val time = LocalTime(date).toString("HH:mm")
      val fullname = x.fullName
      val idt = x.id
      s"$time Uhr: $fullname https://lichess.org/tournament/$idt\n"

    tlist.map(m)

  /**
   * Gets a list of team's Lichess Swiss tournaments on a day.
   * @param tDate the day the list relates to. Defaults to today.
   * @return a String, one line per tournament, with the starting time, the name, and the link to the tournament
   */
  private def getLichessSwiss(session: LichessApi, tDate: LocalDate = LocalDate()): List[de.qno.tournamentadmin.LichessSwissTournamentListEntry] =
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

  private def makeTwitterKey(cred: TwitterCredentials): String =
    java.net.URLEncoder.encode(cred.xApiKeySecret, java.nio.charset.Charset.defaultCharset()) + "&" + java.net.URLEncoder.encode(cred.xAccessTokenSecret, java.nio.charset.Charset.defaultCharset())

  /**
   * Construct and send a message announcing todays tournaments to
   * - the Lichess team
   * - the Bluesky account
   */
  private def sendMessages(): Unit =
    // TODO: pre and post text from file
    // Lichess has to be defined, else no tournaments!
    val preAnnouncementText = "Heutige Turniere:"
    val tournamenAnnouncementText = (getLichessArenasDates(getLichessArenas(lichessSession))
      ::: TournamentAdmin.getLichessSwiss(lichessSession))
      .sorted.foldLeft(preAnnouncementText)(_ + "\n" + _)
    val preResultText = "Ergebnisse von gestern:"
    
    if tournamenAnnouncementText.nonEmpty then
      // Because a lichess account exists, announcements will always happen
      TournamentInstance.create(lichessSession)
      lichessSession.sendMessage(tournamenAnnouncementText)

      blueskyCreds match
        case Success(cred) =>
          val bsSession = Bluesky.createSession(cred.bsUser, cred.bsPassword)
          Bluesky.createRecord(bsSession, tournamenAnnouncementText)
        case _ => {}

      twitterCreds match
        case Success(cred) =>
          Twitter.createPost(cred, tournamenAnnouncementText, makeTwitterKey(cred))
        case _ => {}

      print(tournamenAnnouncementText)
    end if

  @main
  def main(): Unit =
    sendMessages()