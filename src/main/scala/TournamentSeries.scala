package de.qno.tournamentadmin

import TournamentAdmin.*
import TournamentAdmin.ChessPlatform.lichess
import TournamentAdmin.TournamentType.{arena, swiss}

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
    os.write.over(TournamentAdmin.pathToResources / "series.json", write(List(UntitledTuesday, WarmUp).sortBy(_.index)))

  def init(): List[TournamentSeries] =
    read[List[TournamentSeries]](os.read(TournamentAdmin.pathToResources / "series.json"))
