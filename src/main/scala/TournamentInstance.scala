package de.qno.tournamentadmin

import sttp.client4.*
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*
import TournamentInstance.*

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

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
    TournamentSeries.seriesList.find(this.index == _.index)

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
  var instances: List[TournamentInstance] = init()
  given Ordering[TournamentInstance] =
    Ordering.fromLessThan((first, second) => first.date.isBefore(second.date))

  object RWgivens:
    private val fmt = ISODateTimeFormat.dateTime()
    given ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
      dt => dt.toString,
      dtstr => fmt.parseDateTime(dtstr.str)
    )
  import RWgivens.given

  /**
   * creates next tournament of given tournament, if it is less than 30 days in the future.
   *
   * @param nT the given tournament
   * @param acc a list of previously created tournaments of the same series
   * @return only the newest created tournament of that series together with the series index
   */
  @tailrec
  def nextNext(nT: TournamentInstance, acc: List[Response[Either[String, String]]]): (TournamentInstance, String) =
    val theNextTournament = nT.nextTournament
    val resp = theNextTournament.createInstance(theNextTournament.createMap)
    if nT.date.isAfter(DateTime.now().plusDays(30)) then
      acc match
        case List() => (theNextTournament, "")
        case x :: xs => // TODO: transferring ALL created instances to the Admin calendar
          x.body match
            case Right(jso: String) => (theNextTournament, jso)
            case _ => throw IllegalArgumentException("The required tournament could not be created.")
    else
      nextNext(theNextTournament, resp :: acc)

  /**
   * directs the creation of new tournaments.
   * 
   * Side effects on TournamentInstance.instances and TournamentAdmin.nextTournaments
   */
  def creation(): Unit =
    val responses =
      for
        instance <- instances
      yield
        nextNext(instance, List[Response[Either[String, String]]]())
    instances = responses.sortBy(_._1.index).map(_._1)
    TournamentAdmin.add(responses.map(_._2))

  def save(responses: List[(TournamentInstance, String)]): Unit =
    os.write.over(TournamentAdmin.pathToResources / "instances.json", write(instances))
    TournamentAdmin.nextTournaments.addAll(responses.map(_._2))

  def init(): List[TournamentInstance] =
    read[List[TournamentInstance]](os.read(TournamentAdmin.pathToResources / "instances.json"))

  @main
  def main(): Unit =
    println(TournamentAdmin.nextTournaments)
    println(TournamentSeries.seriesList)
    println(instances)