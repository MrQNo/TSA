package de.qno.tournamentadmin

import org.joda.time.*
import sttp.client4.*
import upickle.default.*

object Bluesky:
  var refreshToken: String = _

  def createSession(user: String, password: String): (String, String) =
    val body = write(Map(
      "identifier" -> user,
      "password" -> password
    ))
    val jsonResponse: ujson.Value = ujson.read(
      basicRequest
        .body(body)
        .post(uri"https://public.api.bsky.app/xrpc/com.atproto.server.createSession")
        .response(asString.getRight)
        .send(DefaultSyncBackend())
        .body
    )
    refreshToken = jsonResponse("refreshJwt").str
    (jsonResponse("accessJwt").str, jsonResponse("handle").str)

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

  def createRecord(accessToken: String, handle: String, text: String): Boolean =
    val body = write(Map(
      "repo" -> handle,
      "collection:" -> "app.bsky.feed.post",
      "record" -> write(Map(
        "text" -> text,
        "createdAt" -> DateTime(DateTimeZone.getDefault).toString
      ))
    ))
    val jsonResponse: ujson.Value = ujson.read(
      basicRequest
      .auth.bearer(accessToken)
      .body(body)
      .post(uri"https://public.api.bsky.app/xrpc/com.atproto.repo.createRecord")
      .response(asString.getRight)
      .send(DefaultSyncBackend())
      .body
    )
    jsonResponse("validationStatus").str match
      case "valid" => true
      case _ => false
