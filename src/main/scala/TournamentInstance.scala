package de.qno.tournamentadmin

import sttp.client4.*
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*
import TournamentInstance.{nextNext, untitledTuesday, warmUp}

import scala.annotation.tailrec

/**
 * describes a tournament instance
 * 
 * @param series: a TournamentInstance that has all constant information
 * 
 */
case class TournamentInstance(number: Int,
                              date: DateTime,
                              pointerTimes: Int,
                              pointerDays: Int,
                              series: TournamentSeries) derives ReadWriter:

  def nextTournament: TournamentInstance =
    TournamentInstance(number+1,
      date.plusDays(series.nextDays(pointerDays)),
      (pointerTimes + 1) % series.limits.length,
      (pointerDays + 1) % series.nextDays.length,
      series)

  /**
   * creates the property map to transmit for creation
   * @return the map
   */
  def createMap: Map[String, String] =
    Map(
      "name" -> s"${this.number}. ${this.series.title}",
      s"${series.apiStrings.time}" -> series.limits(pointerTimes).toString,
      s"${series.apiStrings.increment}" -> series.increments(pointerTimes).toString,
      s"${series.apiStrings.duration}" -> series.duration.toString,
      s"${series.apiStrings.startdate}" -> date.getMillis.toString,
      s"description" -> series.description
    ) ++ series.additionalConds

  /**
   * creates the tournament
   *
   * @param creationMap API-conform map of properties
   * @return a Either[String, String] The Right string is in JSON format.
   */
  def createInstance(creationMap: Map[String, String]): Response[Either[String, String]] =
    val composedUrl = s"${series.apiStrings.base}${series.apiStrings.pairingAlgorithm}${series.apiStrings.createString}"
    val resp = basicRequest
      .auth.bearer(TournamentAdmin.token)
      .body(creationMap)
      .post(uri"${composedUrl}")
      .response(asString)
      .send(DefaultSyncBackend())
    resp

object TournamentInstance:
  given Ordering[TournamentInstance] =
    Ordering.fromLessThan((first, second) => first.date.isBefore(second.date))

  object RWgivens:
    private val fmt = ISODateTimeFormat.dateTime()
    given ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
      dt => dt.toString,
      dtstr => fmt.parseDateTime(dtstr.str)
    )
  import RWgivens.given

  val warmUp: TournamentInstance = TournamentInstance(number = 103,
    date = new DateTime(2024, 8, 29, 19, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 2,
    pointerDays = 1,
    WarmUp
  )

  val untitledTuesday: TournamentInstance = TournamentInstance(number = 49,
    date = new DateTime(2024, 8, 6, 20, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 0,
    pointerDays = 0,
    UntitledTuesday
  )

  def writeInstances(xs: List[TournamentInstance]): Unit =
    val jsonInstances = write(xs)
    os.write.over(TournamentAdmin.pathToResources / "instances.json", jsonInstances)

  @tailrec
  def nextNext(nT: TournamentInstance, acc: List[Response[Either[String, String]]]): List[Response[Either[String, String]]] =
    if nT.date.isAfter(DateTime.now().plusDays(30)) then
      acc
    else
      val theNextTournament = nT.nextTournament
      val resp = theNextTournament.createInstance(theNextTournament.createMap)
      nextNext(theNextTournament, resp :: acc)

  @main
  def main(args: String*): Unit =
    val s = "uiae"
    val responses =
      for
        instance <- List[TournamentInstance](warmUp, untitledTuesday)
      yield
        nextNext(instance, List[Response[Either[String, String]]]())
    for
      ser <- responses
      resp <- ser
    do
      println(resp.body match
        case Left(err: String) => err
        case Right(jso: String) => jso)


