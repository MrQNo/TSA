package de.qno.tournamentadmin

import TournamentAdmin.TournamentType

import upickle.default.*

/** describes a tournament series
 * 
 * class is stateless, immutable, and threadsafe
 * 
 * Two instances for own use added. Deprecated; will be replaced by JSON import.
 */
case class TournamentSeries (index: Int,
                             tournamentType: TournamentType,
                             title: String,
                             duration: Int,
                             description: String,
                             limits: Array[Int],
                             increments: Array[Int],
                             nextDays: Array[Int]) derives ReadWriter
end TournamentSeries

object TournamentSeries:
  var seriesList: List[TournamentSeries] = init()
  
  def save(): Unit =
    os.write.over(os.pwd / "series.json", write(seriesList.sortBy(_.index)))

  def init(): List[TournamentSeries] =
    read[List[TournamentSeries]](os.read(os.pwd / "series.json"))
