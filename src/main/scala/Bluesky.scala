package de.qno.tournamentadmin

import scala.compiletime.uninitialized

import org.joda.time.*
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*

import de.qno.tournamentadmin

case class BlueskyCredentials(bsUser: String, bsPassword: String)

case class BlueskySession(accessJwt: String,
                          refreshJwt: String,
                          handle: String,
                          did: String = "",
                          didDoc: Map[String, ujson.Value] = Map(),
                          email: String = "",
                          emailConfirmed: Boolean = false,
                          emailAuthFactor: Boolean = false,
                          active: Boolean = true,
                          status: String = "") derives ReadWriter:

  private def createRecord(text: String, rkey: String = "", validate: Boolean = false): Response =
    val message = Record(text, DateTime(DateTimeZone.getDefault).toString)
    val newRecord = CreateRecord(repo = handle, collection = "app.bsky.feed.post", record = message)
  
    basicRequest
      .auth.bearer(accessJwt)
      .contentType("application/json")
      .body(write(newRecord))
      .post(uri"https://bsky.social/xrpc/com.atproto.repo.createRecord")
      .response(asJson[Response])
      .send(DefaultSyncBackend())
      .body.getOrElse(Response("", "", Commit("", ""), ""))
    
  private def createReply(text: String, rkey: String = "", validate: Boolean = false, rootPost: Response, parentPost: Response): Response =
    val root = PostReference(rootPost.uri, rootPost.cid)
    val parent = PostReference(parentPost.uri, parentPost.cid)
    val replyObject = ReplyObject(root, parent)
    val replyRecord = ReplyRecord(text, DateTime(DateTimeZone.getDefault).toString, replyObject)
    val newRecord = CreateReplyRecord(repo = handle, collection = "app.bsky.feed.post", record = replyRecord)

    basicRequest
      .auth.bearer(accessJwt)
      .contentType("application/json")
      .body(write[CreateReplyRecord](newRecord))
      .post(uri"https://bsky.social/xrpc/com.atproto.repo.createRecord")
      .response(asJson[Response])
      .send(DefaultSyncBackend())
      .body.getOrElse(Response("", "", Commit("", ""), ""))

  /**
   * Sends a List of Strings as a thread of messages.
   * 
   * If a String is longer than the allowed size of a message, it is replaced by variou strings created by splitting it at empty lines.
   * 
   * @param messages a List of Strings containing the messages to post
   */
  def sendMessages(messages: List[String]): Unit =
    val messagesIterator = Bluesky.shortenMessages(messages).iterator
    val root: Response = createRecord(messagesIterator.next())
    var parent: Response = root
    while messagesIterator.hasNext do {
      parent = createReply(text = messagesIterator.next(), rootPost = root, parentPost = parent)
    }

case class Response(uri: String, cid: String, commit: Commit, validationStatus: String = "") derives ReadWriter

case class Commit(cid: String, rev: String) derives ReadWriter

case class Record(text: String, createdAt: String) derives ReadWriter

case class ReplyRecord(text: String, createdAt: String, reply: ReplyObject) derives ReadWriter

case class ReplyObject(root: PostReference, parent: PostReference) derives ReadWriter

case class PostReference(uri: String, cid: String) derives ReadWriter

private case class CreateRecord(repo: String, collection: String, rkey: String = "", validate: Boolean = false, record: Record) derives ReadWriter

private case class CreateReplyRecord(repo: String, collection: String, rkey: String = "", validate: Boolean = false, record: ReplyRecord) derives ReadWriter

object Bluesky:
  private val BLUESKY_MAX_LENGTH = 300

  private var refreshToken: String = uninitialized

  def createSession(user: String, password: String, authFactorToken: String = ""): BlueskySession =
    val body = write(Map(
      "identifier" -> user,
      "password" -> password,
      "authFactorToken" -> authFactorToken
    ))
    basicRequest
      .contentType("application/json")
      .body(body)
      .post(uri"https://bsky.social/xrpc/com.atproto.server.createSession")
      .response(asJson[tournamentadmin.BlueskySession])
      .send(DefaultSyncBackend())
      .body.getOrElse(BlueskySession("", "", ""))

  def refreshSesson(): String =
    val jsonResponse: ujson.Value = ujson.read(
      basicRequest
      .auth.bearer(refreshToken)
      .post(uri"https://public.api.bsky.app/xrpc/com.atproto.server.refreshSession")
      .response(asStringAlways)
      .send(DefaultSyncBackend())
      .body
    )
    refreshToken = jsonResponse("refreshJwt").str
    jsonResponse("accessJwt").str

  /**
   * Split message strings that are too long for social media in smaller parts at empty lines
   *
   * @param messages  An Array of message Strings
   * @return an Array of shortened message Strings
   */
  def shortenMessages(messages: List[String]): List[String] =
    messages.flatMap({
      m =>
        if m.length > BLUESKY_MAX_LENGTH then
          m.split("\n\n")
        else
          List(m)
    })
    
    