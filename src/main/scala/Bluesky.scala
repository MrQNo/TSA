package de.qno.tournamentadmin

import de.qno.tournamentadmin
import org.joda.time.*
import sttp.client4.*
import upickle.default.*
import sttp.client4.upicklejson.default.*

import scala.compiletime.uninitialized

case class BlueskySession(accessJwt: String,
                          refreshJwt: String,
                          handle: String,
                          did: String = "",
                          didDoc: Map[String, ujson.Value] = Map(),
                          email: String = "",
                          emailConfirmed: Boolean = false,
                          emailAuthFactor: Boolean = false,
                          active: Boolean = true,
                          status: String = "")

object BlueskySession:
  implicit val bsSRW: ReadWriter[BlueskySession] = macroRW
  
case class BCRResponse(uri: String, cid: String, commit: BCRRCommit, validationStatus: Option[String])

object BCRResponse:
  implicit val bccrRW: ReadWriter[BCRResponse] = macroRW
  
case class BCRRCommit(cid: String, rev: String)

object BCRRCommit:
  implicit val bccrcRW: ReadWriter[BCRRCommit] = macroRW

case class BlueskyRecord(text: String, createdAt: String)

object BlueskyRecord:
  implicit val bsRW: ReadWriter[BlueskyRecord] =  macroRW

// TODO: collection with default value to JSON
private case class BlueskyCreateRecord(repo: String, collection: String, rkey: String = "", validate: Option[Boolean] = None, record: BlueskyRecord)

private object BlueskyCreateRecord:
  implicit val bscrRW: ReadWriter[BlueskyCreateRecord] = macroRW

object Bluesky:
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
      .response(asJson[tournamentadmin.BlueskySession].getRight)
      .send(DefaultSyncBackend())
      .body

  def refreshSesson(): String =
    val jsonResponse: ujson.Value = ujson.read(
      basicRequest
      .auth.bearer(refreshToken)
      .post(uri"https://public.api.bsky.app/xrpc/com.atproto.server.refreshSession")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
    )
    refreshToken = jsonResponse("refreshJwt").str
    jsonResponse("accessJwt").str

  def createRecord(session: BlueskySession, text: String, rkey: String = "", validate: Option[Boolean] = None): String =
    val message = BlueskyRecord(text, DateTime(DateTimeZone.getDefault).toString)
    val newRecord = BlueskyCreateRecord(repo = session.handle, collection = "app.bsky.feed.post", record = message)
    val newRecordString = write(newRecord)

    basicRequest
      .auth.bearer(session.accessJwt)
      .contentType("application/json")
      .body(newRecordString)
      .post(uri"https://bsky.social/xrpc/com.atproto.repo.createRecord")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body