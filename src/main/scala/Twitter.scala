package de.qno.tournamentadmin

import org.joda.time.DateTime
import upickle.default.*
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import sttp.model.*

import java.net.URLEncoder
import javax.crypto.spec.SecretKeySpec
import scala.util.Random

case class XCreatePostResponse(data: XCreatePostResponse.XCreatePostResponseData)

object XCreatePostResponse:
  implicit val xcprRW: ReadWriter[XCreatePostResponse] = macroRW

  case class XCreatePostResponseData(id: String, text: String)

  object XCreatePostResponseData:
    implicit val xcprdRW: ReadWriter[XCreatePostResponse.XCreatePostResponseData] = macroRW

/**
 * With thx to Kevin Williams
 *
 */
object Auth:
  private def urlEncode(text: String): String =
    URLEncoder.encode(text, java.nio.charset.Charset.defaultCharset())

  private def generateNonce: String =
    Random.alphanumeric.take(15).mkString

  private def canonicalize(data: Map[String, String]) : String =
    data.map(x => urlEncode(x._1) -> urlEncode(x._2)) // percent encode keys and values
      .toSeq.sortBy(_._1) // sort alphabetically
      .map(x => s"${x._1}=${x._2}") // unite key and value with =
      .mkString("&") // unite all with &

  private def canonicalizeWithQuotes(data: Map[String, String]) : String =
    data.map(x => urlEncode(x._1) -> urlEncode(x._2)) // percent encode keys and values
      .toSeq.sortBy(_._1) // sort alphabetically
      .map(x => s"${x._1}=\"${x._2}\"") // unite key and value with =
      .mkString("&") // unite all with &

  /**
   * With thx to Aravind_G
   * @param text The text to sign
   * @param secretKey the secret key to sign with
   * @return The HMAC-SHA1 signature
   * @see https://community.gatling.io/t/hmac-sha1-signature-generation-using-scala/5844
   */
  private def sha1sign (text: String, secretKey: String):  String =
    val shaHash = javax.crypto.Mac.getInstance("HmacSHA1")
    shaHash.init(new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA1"))
    val signatureHash = shaHash.doFinal(text.getBytes("UTF-8"))
    java.util.Base64.getEncoder.encodeToString(signatureHash)

  /**
   * Produces a signed OAuth1 header.
   * With thx to Kevin Williams and Daniel DeGroff
   * @param key The secret key. Usually combines consumerSecret&tokenSecret
   * @return The complete signed Authorization header as a String
   * @see https://medium.com/@kevinwilliams.dev/posting-to-x-twitter-with-oauth-1-0-8d4de172cfa6
   * @see https://fusionauth.io/articles/oauth/oauth-v1-signed-requests
   */
  def signedHeader(key: String): Header =
    val collectAuthHeaderMap = Map(
      "oauth_consumer_key" -> TournamentAdmin.xApiKey,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> (DateTime().getMillis / 1000).toString,
      "oauth_nonce" -> generateNonce,
      "oauth_token" -> TournamentAdmin.xAccesToken,
      "oauth_version" -> "1.0"
    )

    val request = "POST" + "&" + urlEncode(Twitter.createPostEndpoint) + "&" + urlEncode(canonicalize(collectAuthHeaderMap))
    val signature = sha1sign(request, key)
    val signatureDataTupel: (String, String) = ("oauth_signature", signature) // add signature to data
    val oneStringSignedHeader = "OAuth " + canonicalizeWithQuotes(collectAuthHeaderMap + signatureDataTupel).replace("&", ", ")
    Header("Authorization", oneStringSignedHeader)

object Twitter:
  private case class XCreatePostBody(text: String)

  private object XCreatePostBody:
    implicit val xcpbRW: ReadWriter[XCreatePostBody] = macroRW

  val createPostEndpoint = "https://api.x.com/2/tweets"

  def createPost(text: String, key: String): XCreatePostResponse =
    val textObj = XCreatePostBody(text)
    val h = Auth.signedHeader(key)
    println(h)
    basicRequest
      .header(h)
      .contentType("application/json")
      .body(write(textObj))
      .post(uri"$createPostEndpoint")
      .response(asJson[XCreatePostResponse].getRight)
      .send(DefaultSyncBackend())
      .body