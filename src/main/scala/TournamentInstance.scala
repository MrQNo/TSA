package de.qno.tournamentadmin

import sttp.client4.*
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*
import TournamentInstance.*

import scala.annotation.tailrec

/**
 * describes a tournament instance
 *
 * @param index same index as the corresponding series
 */
case class TournamentInstance(index: Int,
                              number: Int,
                              date: DateTime,
                              pointerTimes: Int,
                              pointerDays: Int) derives ReadWriter:

  /**
   * Calculates the next TournamentInstance after this
   * @return the next TournamentInstance
   */
  def nextTournament: TournamentInstance =
    TournamentInstance(index, number+1,
      date.plusDays(series.nextDays(pointerDays)),
      (pointerTimes + 1) % series.limits.length,
      (pointerDays + 1) % series.nextDays.length)

  def series: TournamentSeries =
    index match
      case 0 => UntitledTuesday
      case 1 => WarmUp

  /**
   * creates the property map to transmit for creation
   *
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
      .post(uri"$composedUrl")
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

  val warmUp: TournamentInstance = TournamentInstance(index = 1, number = 106,
    date = new DateTime(2024, 9, 9, 19, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 2,
    pointerDays = 1
  )

  val untitledTuesday: TournamentInstance = TournamentInstance(index = 0, number = 54,
    date = new DateTime(2024, 9, 10, 20, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 0,
    pointerDays = 0
  )

  /**
   * creates next tournament of given tournament, if it is less than 30 days in the future.
   *
   * @param nT the given tournament
   * @param acc a list of previously created tournaments of the same series
   * @return only the newest created tournament of that series together with the series index
   */
  @tailrec
  def nextNext(nT: TournamentInstance, acc: List[Response[Either[String, String]]]): (Int, String) =
    if nT.date.isAfter(DateTime.now().plusDays(30)) then
      acc match
        case List() => (nT.index, "")
        case x :: xs => // TODO: transferring ALL created instances to the Admin calendar
          x.body match
            case Right(jso: String) => (nT.index, jso)
            case _ => throw IllegalArgumentException("The required tournament could not be created.")
    else
      val theNextTournament = nT.nextTournament
      val resp = theNextTournament.createInstance(theNextTournament.createMap)
      nextNext(theNextTournament, resp :: acc)

  @main
  def main(args: String*): Unit =
    // TODO: This works not correct. I need two things: the json responses for the Admin calendar AND the TournamentInstance of the resp. last instance.
    val responses = // contains the latest instance of each series
      for
        instance <- List[TournamentInstance](warmUp, untitledTuesday)
      yield
        nextNext(instance, List[Response[Either[String, String]]]())
    os.write.over(TournamentAdmin.pathToResources / "instances.json", write(responses.sortBy(_._1).map(_._2)))
    os.write.over(TournamentAdmin.pathToResources / "series.json", write(List(UntitledTuesday, WarmUp).sortBy(_.index)))
