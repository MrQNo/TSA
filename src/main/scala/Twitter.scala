package de.qno.tournamentadmin

import org.joda.time.DateTime
import upickle.default.*
import sttp.client4.*
import sttp.client4.upicklejson.default.*
import sttp.model.*

import java.net.URLEncoder
import javax.crypto.spec.SecretKeySpec
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
    Random.alphanumeric.take(15).mkString

  def sortData(data: Map[String, String]): Seq[(String, String)] =
    data.toSeq.sortBy(_._1) // sort alphabetically
  
  def reformatData(data: Seq[(String, String)]): Seq[String] =   
    data.map(x => s"${x._1}=${x._2}")
      
  def reformatSignedData(data: Seq[(String, String)]): Seq[String] =
    data.map(x => s"${x._1}=\"${urlEncode(x._2)}\"")  

  /**
   * With thx to Aravind_G
   * https://community.gatling.io/t/hmac-sha1-signature-generation-using-scala/5844
   * @param text
   * @return
   */
  def sha1sign (text: String):  String =
    val shaHash = javax.crypto.Mac.getInstance("HmacSHA1")
    val secretKey = s"${urlEncode(TournamentAdmin.xApiKeySecret)}&${urlEncode(TournamentAdmin.xAccessTokenSecret)}"
    val signingKey = new SecretKeySpec(secretKey.getBytes, "HmacSHA1")
    shaHash.init(signingKey)
    val signatureHash = shaHash.doFinal(text.getBytes("UTF-8"))
    val baseEncoder = java.util.Base64.getEncoder
    val signatureString = (for byte <- signatureHash yield f"$byte%02X").mkString
    baseEncoder.encodeToString(signatureString.getBytes)
  
  def signedHeader: Header =
    val collectAuthHeaderMap = Map(
      "oauth_consumer_key" -> TournamentAdmin.xApiKey,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> DateTime().getMillis.toString,
      "oauth_nonce" -> generateNonce,
      "oauth_token" -> TournamentAdmin.xAccesToken,
      "oauth_version" -> "1.0"
    )

    val sortedData = sortData(collectAuthHeaderMap)
    val reformattedData = reformatData(sortedData)
    val oneStringData = "POST" + "&" + urlEncode(Twitter.createPostEndpoint) + "&" + urlEncode(reformattedData.mkString("&"))
    val signature = sha1sign(oneStringData)
    val signatureDataTupel: (String, String) = ("oauth_signature", signature) // add signature to data
    val sortedSignedHeader = sortData(collectAuthHeaderMap + signatureDataTupel)
    val reformattedSignedHeader = reformatSignedData(sortedSignedHeader)
    val oneStringSignedHeader = "OAuth " + reformattedSignedHeader.mkString(", ")
    Header("Authorization", oneStringSignedHeader)

object Twitter:
  val createPostEndpoint = "https://api.x.com/2/tweets"

  def createPost(text: String): XCreatePostResponse =
    val textObj = XCreatePostBody(text)
    val h = Auth.signedHeader
    println(h)
    basicRequest
      .header(h)
      .contentType("application/json")
      .body(write(textObj))
      .post(uri"$createPostEndpoint")
      .response(asJson[XCreatePostResponse].getRight)
      .send(DefaultSyncBackend())
      .body

  @main
  def showHeader: Unit =
    createPost("Test")
