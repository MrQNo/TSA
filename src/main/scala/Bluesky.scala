package de.qno.tournamentadmin

import org.joda.time.*
import sttp.client4.*
import upickle.default.*
import upickle.default.{ReadWriter, macroRW}

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

case class BlueskyRecord(text: String, createdAt: String)

object BlueskyRecord:
  implicit val bsRW: ReadWriter[BlueskyRecord] =  macroRW

// TODO: collection with default value to JSON
case class BlueskyCreateRecord(repo: String, collection: String, rkey: String = "", validate: Option[Boolean] = None, record: BlueskyRecord)

object BlueskyCreateRecord:
  implicit val bscrRW: ReadWriter[BlueskyCreateRecord] = macroRW

// TODO: validationStatus to Option[String]
case class BlueskyCreateRecordResponse(uri: String, cid: String, commit: BlueskyCreateRecordResponseCommit, validationStatus: String)

object BlueskyCreateRecordResponse:
  implicit val bsCRR: ReadWriter[BlueskyCreateRecordResponse] = macroRW

case class BlueskyCreateRecordResponseCommit(cid: String, rev: String)

object BlueskyCreateRecordResponseCommit:
  implicit val bsCRRC: ReadWriter[BlueskyCreateRecordResponseCommit] = macroRW

object Bluesky:
  var refreshToken: String = _

  def createSession(user: String, password: String, authFactorToken: String = ""): BlueskySession =
    val body = write(Map(
      "identifier" -> user,
      "password" -> password,
      "authFactorToken" -> authFactorToken
    ))
    read[BlueskySession](
      basicRequest
        .contentType("application/json")
        .body(body)
        .post(uri"https://bsky.social/xrpc/com.atproto.server.createSession")
        .response(asString.getRight)
        .send(DefaultSyncBackend())
        .body
    )

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

  def createRecord(session: BlueskySession, text: String, rkey: String = "", validate: Option[Boolean] = None): BlueskyCreateRecordResponse =
    val message = BlueskyRecord(text, DateTime(DateTimeZone.getDefault).toString)
    val newRecord = BlueskyCreateRecord(repo = session.handle, collection = "app.bsky.feed.post", record = message)
    val newRecordString = write(newRecord)

    read[BlueskyCreateRecordResponse](
      basicRequest
      .auth.bearer(session.accessJwt)
      .contentType("application/json")
      .body(newRecordString)
      .post(uri"https://bsky.social/xrpc/com.atproto.repo.createRecord")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
    )