package de.qno.tournamentadmin

import sttp.client4.*
import sttp.client4.upicklejson.default.*
import upickle.default.*

import scala.annotation.tailrec

case class MastodonStatusResponse(id: String, created_at: String) derives ReadWriter

object Mastodon:
  @tailrec
  def post(accessToken: String, messages: List[String], replyId: String = ""): Int =
    val composedUrl = "https://mastodon.berlin/api/v1/statuses/"
    val statusMap = Map(
      "status" -> messages.head
    )
    val stMap = if replyId != "" then 
        statusMap + ("in_reply_to_id" -> replyId)
    else
      statusMap
    val resp = basicRequest
      .auth.bearer(accessToken)
      .body(stMap)
      .post(uri"$composedUrl")
      .response(asJson[MastodonStatusResponse])
      .send(DefaultSyncBackend())
      .body
    resp match
      case Right(st) =>
        messages match
          case x::Nil => 0
          case x::taillist =>
            post(accessToken, taillist ,st.id)
          case _ => 1
      case Left(e) =>
        throw e
        
    
    
    
