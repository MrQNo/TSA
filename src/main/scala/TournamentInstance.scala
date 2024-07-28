package de.qno.tournamentadmin

import sttp.client4.*
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*

/**
 * describes a tournament instance
 * 
 * @param series: a TournamentInstance that has all constant information
 * 
 */
case class TournamentInstance(id: Int,
                              number: Int,
                              nextDate: DateTime,
                              pointerTimes: Int,
                              pointerDays: Int,
                              series: TournamentSeries) derives ReadWriter:

  def createTournaments(): Unit =
    while nextDate.isBefore(DateTime.now().plusMonths(1))
    do
      TournamentInstance.nextNext(this)

object TournamentInstance:
  given Ordering[TournamentInstance] =
    Ordering.fromLessThan((first, second) => first.nextDate isBefore(second.nextDate))

  object RWgivens:
    private val fmt = ISODateTimeFormat.dateTime()

    given ReadWriter[DateTime] = readwriter[ujson.Value].bimap[DateTime](
      dt => dt.toString,
      dtstr => fmt.parseDateTime(dtstr.str)
    )

  import RWgivens.given


  val warmUp: TournamentInstance = TournamentInstance(0,
    number = 96,
    nextDate = new DateTime(2024, 8, 5, 19, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 2,
    pointerDays = 1,
    WarmUp
  )

  val untitledTuesday: TournamentInstance = TournamentInstance(1,
    number = 50,
    nextDate = new DateTime(2024, 8, 13, 20, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 0,
    pointerDays = 0,
    UntitledTuesday
  )

  private def createMap(inst: TournamentInstance): Map[String, String] =
    Map(
      "name" -> s"$this.number. $this.series.title",
      s"${inst.series.apiStrings.time}" -> inst.series.limits(inst.pointerTimes).toString,
      s"${inst.series.apiStrings.increment}" -> inst.series.increments(inst.pointerTimes).toString,
      s"${inst.series.apiStrings.duration}" -> inst.series.duration.toString,
      s"${inst.series.apiStrings.startdate}" -> inst.nextDate.getMillis.toString,
      s"description" -> inst.series.description
    ) ++ inst.series.additionalConds
  
  private def createNextInstance(inst: TournamentInstance, creationMap: Map[String, String]) =
    basicRequest
      .body(creationMap)
      .post(uri"${inst.series.apiStrings.base}${inst.series.apiStrings.pairingAlgorithm}${inst.series.apiStrings.createString}")
      .response(asString)
      .send(DefaultSyncBackend())
  
  def nextNext(inst: TournamentInstance): TournamentInstance =
    val response = createNextInstance(inst, createMap(inst))
    println(response.body)
    TournamentInstance(0,
      inst.number + 1,
      inst.nextDate.plusDays(inst.series.nextDays(inst.pointerDays)),
      (inst.pointerTimes + 1) % inst.series.limits.length,
      (inst.pointerDays + 1) % inst.series.nextDays.length,
      inst.series)
  end nextNext

@main
def writeInstances: Unit =
  val instances = List(TournamentInstance.warmUp, TournamentInstance.untitledTuesday)
  val jsonInstances = write(instances)
  os.write.over(TournamentAdmin.pathToResources / "instances.json", jsonInstances)

  
  