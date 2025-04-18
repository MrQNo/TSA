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
      
    val results: Iterator[String] = getTournamentInfos(LocalDate().minusDays(1), lichessSession.teamId).iterator
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
  private def sendMessages(messages: List[String]): Unit =
    // TODO: pre and post text from file
    // Lichess has to be defined, else no tournaments!
    println(messages.foldLeft("")(_ + _))
    
    if false then 
      if messages.nonEmpty then
        // Because a lichess account exists, announcements will always happen
        // TournamentInstance.create(lichessSession)
        lichessSession.sendMessage(messages.foldLeft("")(_ + _))
  
        blueskyCreds match
          case Success(cred) =>
            val messagesIterator = messages.iterator
            val bsSession = Bluesky.createSession(cred.bsUser, cred.bsPassword)
            val root: Response = bsSession.createRecord(messagesIterator.next())
            var parent: Response = root
            while messagesIterator.hasNext do 
              parent = bsSession.createReply(text = messagesIterator.next(), rootPost = root, parentPost = parent)
          case _ => 
  
//        twitterCreds match
//          case Success(cred) =>
//            Twitter.createPost(cred, tournamenAnnouncementText, makeTwitterKey(cred))
//          case _ => {}
  
      end if
    end if
    //println(tournamenAnnouncementText)
    
  @main
  def main(): Unit = {
    //TournamentInstance.create(lichessSession)
    sendMessages(prepareMessages())
  }
      