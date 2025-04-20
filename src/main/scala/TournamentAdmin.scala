package de.qno.tournamentadmin

import scala.util.*
import scala.collection.mutable.ListBuffer

import org.joda.time.*

import lichess.LichessApi
import lichess.LichessApi.*

/**
 * Main class of package. Provides static properties, types, and methods
 * 
 * Uses file src/main/resources where access tokens are saved. Do not distribute this file!
 * Change IDs to your need. No other changes should be necessary.
 * Of course you have to edit series.json and instances.json to fit to your tournaments.
 */
object TournamentAdmin:
  
  // private val secrets = os.read.lines(os.pwd / "twitter.token").iterator

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

//  private def makeTwitterKey(cred: TwitterCredentials): String =
//    java.net.URLEncoder.encode(cred.xApiKeySecret, java.nio.charset.Charset.defaultCharset()) + "&" + java.net.URLEncoder.encode(cred.xAccessTokenSecret, java.nio.charset.Charset.defaultCharset())

  private def prepareMessages(): List[String] =
    val preAnnouncementText = "Heutige Turniere:\n"
    val preResultText = "Ergebnisse von gestern:\n"
    
    val announcements: String = 
      val ann = getTournamentAnnouncements(LocalDate(), lichessSession.teamId)
      if ann == "" then 
        ""
      else
        preAnnouncementText ++ ann
        
    val results: List[String] = getTournamentInfos(LocalDate().minusDays(1), lichessSession.teamId)
    val resultsIterator = results.iterator
    if resultsIterator.hasNext then 
      val result: ListBuffer[String] = ListBuffer(announcements, preResultText + resultsIterator.next())
      while resultsIterator.hasNext do 
        result.addOne(resultsIterator.next())
      result.toList
    end if
    
    announcements :: results  
      
  /**
   * Construct and send a message announcing todays tournaments to
   * - the Lichess team
   * - the Bluesky account
   */
  private def sendMessages(): Unit =
    // TODO: pre and post text from file
    val messages = prepareMessages()
    
    // Because a lichess account exists, announcements will always happen
    TournamentInstance.create(lichessSession)

    if messages.nonEmpty then
      lichessSession.sendMessage(messages.foldLeft("")(_ + _))
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
    println(messages)
    
  @main
  def main(): Unit = {
    // TournamentInstance.create(lichessSession)
    sendMessages()
  }
      