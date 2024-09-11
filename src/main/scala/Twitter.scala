package de.qno.tournamentadmin

import org.joda.time.DateTime
import upickle.default.*
import sttp.client4.*
import sttp.client4.upicklejson.default.*

import java.net.URLEncoder
import scala.util.Random

case class XCreatePostBody(text: String)

object XCreatePostBody:
  implicit val xcpbRW: ReadWriter[XCreatePostBody] = macroRW

case class XCreatePostResponse(data: XCreatePostResponse.XCreatePostResponseData)

object XCreatePostResponse:
  implicit val xcprRW: ReadWriter[XCreatePostResponse] = macroRW

  case class XCreatePostResponseData(id: String, text: String)

  object XCreatePostResponseData:
    implicit val xcprdRW: ReadWriter[XCreatePostResponse.XCreatePostResponseData] = macroRW

/**
 * With thx to Kevin Williams
 * https://medium.com/@kevinwilliams.dev/posting-to-x-twitter-with-oauth-1-0-8d4de172cfa6
 */
object Auth:
  def urlEncode(text: String): String =
    URLEncoder.encode(text, java.nio.charset.Charset.defaultCharset())

  def generateNonce: String =
    Random.nextString(15)

  def sortAndReformatHeader(data: Map[String, String]): Seq[String] =
    data.toSeq.sortBy(_._1) // sort alphabetically
      .map(x => s"${x._1}=${x._2}")

  def joinHeaderSeqToString(data: Seq[String]): String =
    "POST" + urlEncode("&") + urlEncode(Twitter.createPostEndpoint) + urlEncode("&") + (data.head ++ data.tail.map(x => urlEncode("&") + x)).foldLeft("")(_ + _)

  def signHeader: String =
    val collectAuthHeaderMap = Map(
      "oauth_consumer_key" -> TournamentAdmin.xApiKey,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> DateTime().getMillis.toString,
      "oauth_nonce" -> generateNonce,
      "oauth_token" -> TournamentAdmin.xAccesToken,
      "oauth_version" -> "1.0"
    ).map((x, y) => urlEncode(x) -> urlEncode(y))

    val shaHash = java.security.MessageDigest.getInstance("SHA-1")
    val baseEncoder = java.util.Base64.getEncoder

    val signatureString = baseEncoder.encodeToString(shaHash.digest(joinHeaderSeqToString(sortAndReformatHeader(collectAuthHeaderMap)).getBytes))
    val tupel: (String, String) = (urlEncode("oauth_signature"), signatureString)
    joinHeaderSeqToString(sortAndReformatHeader(collectAuthHeaderMap + tupel))

object Twitter:
  val createPostEndpoint = "https://api.x.com/2/tweets"

  def createPost(text: String): XCreatePostResponse =
    val textObj = XCreatePostBody(text)
    basicRequest
      .header("OAuth", Auth.signHeader)
      .contentType("application/json")
      .body(write(textObj))
      .post(uri"$createPostEndpoint")
      .response(asJson[XCreatePostResponse].getRight)
      .send(DefaultSyncBackend())
      .body

  @main
  def showHeader: Unit =
    val h = Auth.signHeader
    println(h)
    createPost("Test")