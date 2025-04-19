package de.qno.tournamentadmin

import scala.util.*
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.ListBuffer
import scala.compiletime.uninitialized

import org.joda.time.*

import lichess.LichessInternalDataTypes.*
import lichess.LichessApi
import lichess.LichessApi.*
import upickle.default.*

/**
 * Main class of package. Provides static properties, types, and methods
 * 
 * Uses file src/main/resources where access tokens are saved. Do not distribute this file!
 * Change IDs to your need. No other changes should be necessary.
 * Of course you have to edit series.json and instances.json to fit to your tournaments.
 */
object TournamentAdmin:
  val TWITTER_MAX_LENGTH = 280
  
  private val secrets = os.read.lines(os.pwd / "twitter.token").iterator

  private val lichessSecretsPath = os.pwd / "lichess.token"
  private val lichessSecrets = os.read.lines(lichessSecretsPath).iterator
  private val lichessSession = LichessApi(lichessSecrets.next(), lichessSecrets.next())

  private val blueskySecretsPath = os.pwd / "bluesky.token"
  private val blueskyCreds: Option[BlueskyCredentials] = 
    if os.exists(blueskySecretsPath) then
      try
        val blueskySecrets = os.read.lines(blueskySecretsPath).iterator
        Some(BlueskyCredentials(blueskySecrets.next(), blueskySecrets.next()))
      catch
        case _ => None
    else
      None  
    
//  private val twitterSecretsPath = os.pwd / "twitter.token"
//  private val twitterCreds: Option[TwitterCredentials] = 
//    if os.exists(twitterSecretsPath) then
//      try
//        val twitterSecrets = os.read.lines(twitterSecretsPath).iterator
//        Some(TwitterCredentials(twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next(), twitterSecrets.next()))
//      catch
//        case _ => None
//    else
//      None  

  private def makeTwitterKey(cred: TwitterCredentials): String =
    java.net.URLEncoder.encode(cred.xApiKeySecret, java.nio.charset.Charset.defaultCharset()) + "&" + java.net.URLEncoder.encode(cred.xAccessTokenSecret, java.nio.charset.Charset.defaultCharset())

  private def prepareMessages(): List[String] =
    val preAnnouncementText = "Heutige Turniere:\n"
    val preResultText = "Ergebnisse von gestern:\n"
    
    val announcements: String = getTournamentAnnouncements(LocalDate(), lichessSession.teamId)
    if announcements == "" then 
      val fullAnnouncements = ""
    else
      val fullAnnouncements = preAnnouncementText ++ announcements
      
    val results: Iterator[String] = getTournamentInfos(LocalDate().minusDays(2), lichessSession.teamId).iterator
    if results.hasNext then 
      val result: ListBuffer[String] = ListBuffer(announcements, (preResultText + results.next()))
      while results.hasNext do 
        result.addOne(results.next())
      result.toList
    else
     List("")  
    
  /**
   * Construct and send a message announcing todays tournaments to
   * - the Lichess team
   * - the Bluesky account
   */
  private def sendMessages(): Unit =
    // TODO: pre and post text from file
    // Lichess has to be defined, else no tournaments!
    val messages = prepareMessages()
    
    if messages.nonEmpty then
      // Because a lichess account exists, announcements will always happen
      // TournamentInstance.create(lichessSession)
      // lichessSession.sendMessage(messages.foldLeft("")(_ + _))

      blueskyCreds match
        case Some(cred: BlueskyCredentials) =>
          val bsSession = Bluesky.createSession(cred.bsUser, cred.bsPassword)
          bsSession.sendMessages(messages)
        case None => 

//      twitterCreds match
//        case Some(cred) =>
//          val messagesIterator = shortenMessages(messages, TWITTER_MAX_LENGTH).iterator
//          Twitter.createPost(cred, tournamenAnnouncementText, makeTwitterKey(cred))
//        case None => {}

    end if
    //println(tournamenAnnouncementText)
    
  @main
  def main(): Unit = {
    TournamentInstance.create(lichessSession)
    sendMessages()
  }
      