package de.qno.tournamentadmin

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*
import TournamentAdmin.TournamentType
import TournamentAdmin.TournamentType.*

import scala.annotation.tailrec

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
   * creates an online tournament from this TournamentInstance
   * @return ID of created tournament
   */
  def createOnline(session: LichessApi): String =
    series.tournamentType match
      case TournamentType.LichessArena => session.createArena(s"${this.number}. ${this.series.title}",
        series.limits(pointerTimes).toString,
        series.increments(pointerTimes).toString,
        series.duration.toString,
        date.getMillis.toString,
        series.description)
      case LichessSwiss => session.createSwiss(s"${this.number}. ${this.series.title}",
        series.limits(pointerTimes).toString,
        series.increments(pointerTimes).toString,
        series.duration.toString,
        date.getMillis.toString,
        series.description,
        "2200")

object TournamentInstance:
  private val instances: Array[TournamentInstance] = init().toArray

  given Ordering[TournamentInstance] =
    Ordering.fromLessThan((first, second) => first.date.isBefore(second.date))
  
  given dtRW: ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
      dt => dt.toString,
      dtstr => ISODateTimeFormat.dateTime().parseDateTime(dtstr.str)
    )

  /**
   * creates next tournament of given tournament, if it is less than 30 days in the future.
   *
   * @param nT the given tournament
   * @return only the newest created tournament of that series together with the series index
   */
  @tailrec
  private def nextNext(session: LichessApi, nT: TournamentInstance): TournamentInstance =
    val theNextTournament = nT.nextTournament
    if theNextTournament.date.isAfter(DateTime.now().plusDays(30)) then
      nT
    else
      theNextTournament.createOnline(session)
      nextNext(session, theNextTournament)

  /**
   * directs the creation of new tournaments.
   */
  private def creation(session: LichessApi): List[TournamentInstance] =
    val responses =
      for
        instance <- instances
      yield
        nextNext(session, instance)
    responses.sortBy(_.index).toList

  private def save(): Unit =
    os.write.over(TournamentAdmin.pathToResources / "instances.json", write(instances))

  private def init(): List[TournamentInstance] =
    read[List[TournamentInstance]](os.read(TournamentAdmin.pathToResources / "instances.json"))

  def create(session: LichessApi): Unit =
    for
      instance <- creation(session)
    do
      val ind = instance.index
      instances(ind) = instance

    save()