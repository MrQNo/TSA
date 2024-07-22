package de.qno.tournamentadmin

import sttp.client4.*
import com.github.nscala_time.time.Imports.*
import org.joda.time.format.ISODateTimeFormat
import upickle.default.*

import com.github.nscala_time.time.Imports.DateTime

case class TournamentInstance( number: Int,
                               nextDate: DateTime,
                               pointerTimes: Int,
                               pointerDays: Int,
                               series: TournamentSeries
                             ) derives ReadWriter

object TournamentInstance:
  val warmUp: TournamentInstance = TournamentInstance(number = 96,
    nextDate = new DateTime(2024, 8, 5, 19, 1, 0, DateTimeZone.forID("Europe/Berlin")),
    pointerTimes = 2,
    pointerDays = 1,
    WarmUp
  )

  val untitledTuesday: TournamentInstance = TournamentInstance(number = 50,
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
    TournamentInstance(inst.number + 1,
      inst.nextDate.plusDays(inst.series.nextDays(inst.pointerDays)),
      (inst.pointerTimes + 1) % inst.series.limits.length,
      (inst.pointerDays + 1) % inst.series.nextDays.length,
      inst.series)
  end nextNext


  
  