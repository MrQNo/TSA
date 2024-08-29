package de.qno.tournamentadmin

import TournamentEntry.*
import TournamentEntry.ChessPlatform.lichess
import TournamentEntry.TournamentType.{arena, swiss}

import upickle.default.*

/** describes a tournament series
 * 
 * class is stateless, immutable, and threadsafe
 * 
 * Two instances for own use added. Deprecated; will be replaced by JSON import.
 */
case class TournamentSeries (index: Int,
                             platform: ChessPlatform,
                             tournamentType: TournamentType,
                             apiStrings: AdminApi,
                             title: String,
                             duration: Int,
                             description: String,
                             limits: Array[Int],
                             increments: Array[Int],
                             nextDays: Array[Int],
                             additionalConds: Map[String, String]) derives ReadWriter
end TournamentSeries

object TournamentSeries:
  var seriesList: List[TournamentSeries] = init()
  
  def save(): Unit =
    os.write.over(TournamentEntry.pathToResources / "series.json", write(seriesList.sortBy(_.index)))

  def init(): List[TournamentSeries] =
    read[List[TournamentSeries]](os.read(TournamentEntry.pathToResources / "series.json"))
