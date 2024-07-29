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
case class TournamentSeries (platform: ChessPlatform,
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

object UntitledTuesday extends TournamentSeries(ChessPlatform.lichess,
  TournamentType.swiss,
  TournamentAdmin.AdminApi.lichessSwiss,
  "Titelloser Dienstag",
  11,
  "11 Runden Schweizer System 3+2 für Spieler unter Blitzwertung 2200",
  Array(180),
  Array(2),
  Array(7),
  Map("conditions.maxRating.rating" -> "2200",
    "conditions.playYourGames" -> "true")
)

object WarmUp extends TournamentSeries(lichess,
  arena,
  TournamentAdmin.AdminApi.lichessArena,
  "LiLa-Warm-Up",
  55,
  "Warm-Up-Arena für die Lichess Liga (immer mit der gleichen Bedenkzeit)",
  Array(3, 3, 5),
  Array(0, 2, 0),
  Array(3, 4),
  Map("conditions.teamMember.teamId" -> TournamentAdmin.teamID)
)

