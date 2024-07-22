package de.qno.tournamentadmin

import TournamentAdmin.{AdminApi, ChessPlatform, TournamentType, lichessSwiss}

import sttp.client4.*
import com.github.nscala_time.time.Imports.*
import de.qno.tournamentadmin.TournamentAdmin.ChessPlatform.lichess
import de.qno.tournamentadmin.TournamentAdmin.TournamentType.{arena, swiss}
import org.joda .time.format.ISODateTimeFormat
import upickle.default.*

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

object TournamentSeries:
end TournamentSeries

object UntitledTuesday extends TournamentSeries(ChessPlatform.lichess,
    TournamentType.swiss,
    lichessSwiss,
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
  TournamentAdmin.lichessArena,
  "LiLa-Warm-Up",
  55,
  "Warm-Up-Arena für die Lichess Liga (immer mit der gleichen Bedenkzeit)",
  Array(3, 3, 5),
  Array(0, 2, 0),
  Array(3, 4),
  Map("conditions.teamMember.teamId" -> TournamentAdmin.teamID)
)
