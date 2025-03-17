package de.qno.tournamentadmin

import scala.util.*
import org.joda.time.*
import lichess.LichessInternalDataTypes.*
import lichess.LichessApi

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
  val lichessSession = LichessApi(lichessSecrets.next(), lichessSecrets.next())

  private val blueskySecretsPath = os.pwd / "bluesky.token"
  private val blueskySecrets = os.read.lines(blueskySecretsPath).iterator
  private val blueskyCreds: Try[BlueskyCredentials] = Try(BlueskyCredentials(blueskySecrets.next(), blueskySecrets.next()))

  private val twitterSecretsPath = os.pwd / "twitter.token"
  private val twitterSecrets = os.read.lines(twitterSecretsPath).iterator
  private val twitterCreds: Try[TwitterCredentials] = Try(TwitterCredentials(twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next()))

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
    val tournamenAnnouncementText = lichessSession.getTournamentAnnouncementDates().foldLeft(preAnnouncementText)(_ + "\n" + _)
    val preResultText = "Ergebnisse von gestern:"
    
    if false then 
      if tournamenAnnouncementText.nonEmpty then
        // Because a lichess account exists, announcements will always happen
        // TournamentInstance.create(lichessSession)
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
    end if
    println(tournamenAnnouncementText)
    
  @main
  def main(): Unit =
    sendMessages()