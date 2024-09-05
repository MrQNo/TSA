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
 * @param number the running number of the current instance in its series
 * @param date the start date
 * @param pointerTimes the pointer at the ToR of the current instance in the series' ToR array
 * @param pointerDays the pointer at the days until next instance of the current instance in the series' days until next instance array
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
    val corrSer = TournamentSeries.seriesList.find(this.index == _.index)
    corrSer match
      case Some(x) => x
      case None => throw new IllegalStateException("No corresponding series found for this instance.")

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
  def createInstance(creationMap: Map[String, String]): Unit =
    val composedUrl = s"${series.apiStrings.base}${series.apiStrings.pairingAlgorithm}${series.apiStrings.createString}"
    basicRequest
      .auth.bearer(TournamentAdmin.token)
      .body(creationMap)
      .post(uri"$composedUrl")
      .response(asString.getRight)
      .send(DefaultSyncBackend())

object TournamentInstance:
  private val instances: Array[TournamentInstance] = init().toArray

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
   * @return only the newest created tournament of that series together with the series index
   */
  @tailrec
  def nextNext(nT: TournamentInstance): TournamentInstance =
    val theNextTournament = nT.nextTournament
    if theNextTournament.date.isAfter(DateTime.now().plusDays(30)) then
      nT
    else
      theNextTournament.createInstance(theNextTournament.createMap)
      nextNext(theNextTournament)

  /**
   * directs the creation of new tournaments.
   * 
   * Side effects on TournamentInstance.instances and TournamentAdmin.nextTournaments
   */
  def creation(): List[TournamentInstance] =
    val responses =
      for
        instance <- instances
      yield
        nextNext(instance)
    responses.sortBy(_.index).toList

  def save(): Unit =
    os.write.over(TournamentAdmin.pathToResources / "instances.json", write(instances))

  def init(): List[TournamentInstance] =
    read[List[TournamentInstance]](os.read(TournamentAdmin.pathToResources / "instances.json"))

  @main
  def main(): Unit =
    for
      instance <- creation()
    do
      val ind = instance.index
      instances(ind) = instance

    save()
    println(instances)