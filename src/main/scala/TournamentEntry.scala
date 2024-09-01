package de.qno.tournamentadmin

import de.qno.tournamentadmin.TournamentEntry.{TournamentType, ChessPlatform}
import upickle.default.*
import sttp.client4.*
import org.joda.time.*
import de.qno.tournamentadmin.AdminApi.*

import scala.collection.mutable.ListBuffer

case class TournamentEntry(id: String,
                           platform: ChessPlatform,
                           typus: TournamentType) derives ReadWriter

object TournamentEntry:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  var todaYesterday: List[TournamentEntry] = List()
  
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
    val text = new ListBuffer[String]()
    text.addOne("Heutige Turniere:\n")
    var todaysTournameets: List[TournamentEntry] = List()
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "arena.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      val date = new DateTime(json("startsAt").num.toLong)
      val day = new LocalDate(date)
      if (day.equals(new LocalDate())) then
        val time = new LocalTime(date).toString("HH:mm")
        val fullname = json("fullName").str
        val idt = json("id").str
        text.addOne(s"$time Uhr: $fullname ${lichessArena.serv}${lichessArena.pairingAlgorithm}$idt\n")
        val newEntry = TournamentEntry(idt, ChessPlatform.lichess, TournamentType.arena)
        todaysTournameets = todaysTournameets :+ newEntry
    end for
    
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "swiss.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      val date = org.joda.time.DateTime.parse(json("startsAt").str).toLocalDate()
      val day = new LocalDate(date)
      if (day.equals(new LocalDate())) then
        val time = new LocalTime(date).toString("HH:mm")
        val fullname = json("name").str
        val idt = json("id").str
        text.addOne(s"$time Uhr: $fullname ${lichessSwiss.serv}${lichessSwiss.pairingAlgorithm}$idt\n")
        val newEntry = TournamentEntry(idt, ChessPlatform.lichess, TournamentType.arena)
        todaysTournameets = todaysTournameets :+ newEntry
    end for
    
    if (!text.isEmpty) then
      basicRequest
       .auth.bearer(TournamentEntry.token)
       .body(Map("message" -> text.foldLeft("")(_ + _)))
       .post(uri"https://lichess.org/team/deutscher-schachbund-ev-offen/pm-all")
       .response(asString.getRight)
       .send(DefaultSyncBackend())
      print(text.foldLeft("")(_ + _))

