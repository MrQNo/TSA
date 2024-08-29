package de.qno.tournamentadmin

import upickle.default.*
import sttp.client4.*
import org.joda.time.*

import scala.collection.mutable.ListBuffer

case class TournamentEntry()

object TournamentEntry:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  var yesterdaysTournaments: List[String] = _

  enum ChessPlatform derives ReadWriter:
    case chesscom, lichess

  enum TournamentType derives ReadWriter:
    case swiss, arena

  @main
  def readTeamsLichessTournaments: Unit =
    val respA = basicRequest
      .auth.bearer(TournamentEntry.token)
      .get(uri"https://lichess.org/api/team/deutscher-schachbund-ev-offen/arena")
      .response(asString.getRight)
      .send(DefaultSyncBackend())

    os.write.over(TournamentEntry.pathToResources / "arena.json", respA.body)

    val respS = basicRequest
      .auth.bearer(TournamentEntry.token)
      .get(uri"https://lichess.org/api/team/deutscher-schachbund-ev-offen/swiss")
      .response(asString.getRight)
      .send(DefaultSyncBackend())

    os.write.append(TournamentEntry.pathToResources / "swiss.json", respS.body)

  @main
  def chooseTodaysTournaments: Unit =
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "arena.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      if (new DateTime(json("startsAt").num.toLong).toLocalDate().equals(new LocalDate().plusDays(1))) then
        println(tournament)
    end for
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "swiss.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      if (org.joda.time.DateTime.parse(json("startsAt").str).toLocalDate().equals(new LocalDate().plusDays(1))) then
        println(tournament)
    end for

