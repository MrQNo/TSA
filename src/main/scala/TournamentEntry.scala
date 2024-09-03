package de.qno.tournamentadmin

import TournamentEntry.{TournamentType, ChessPlatform}
import upickle.default.*
import sttp.client4.*
import org.joda.time.*
import AdminApi.*

import scala.collection.mutable.ListBuffer

case class TournamentEntry(id: String,
                           platform: ChessPlatform,
                           typus: TournamentType) derives ReadWriter

object TournamentEntry:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val bsUser = "onlineschach@schachbund.de"
  val secrets = os.read.lines(pathToResources / "token.txt")
  val token = secrets.head
  val bsPassword = secrets.tail.head
  
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

    os.write.over(TournamentEntry.pathToResources / "swiss.json", respS.body)

  @main
  def chooseTodaysTournaments: Unit =
    val text = new ListBuffer[String]()
    text.addOne("Heutige Turniere:\n")
    var todaysTournaments: List[TournamentEntry] = List()
    
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "arena.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      val date = new DateTime(json("startsAt").num.toLong)
      val day = new LocalDate(date)
      if day.equals(new LocalDate()) then
        val time = new LocalTime(date).toString("HH:mm")
        val fullname = json("fullName").str
        val idt = json("id").str
        text.addOne(s"$time Uhr: $fullname ${lichessArena.serv}${lichessArena.pairingAlgorithm}/$idt\n")
        val newEntry = TournamentEntry(idt, ChessPlatform.lichess, TournamentType.arena)
        todaysTournaments = todaysTournaments :+ newEntry
    end for
    
    for
      tournament <- os.read.lines.stream(TournamentEntry.pathToResources / "swiss.json")
    do
      val json: ujson.Value = ujson.read(tournament)
      val date = org.joda.time.DateTime.parse(json("startsAt").str).toDateTime(DateTimeZone.getDefault)
      val day = new LocalDate(date)
      if day.equals(new LocalDate()) then
        val time = new LocalTime(date).toString("HH:mm")
        val fullname = json("name").str
        val idt = json("id").str
        text.addOne(s"$time Uhr: $fullname ${lichessSwiss.serv}${lichessSwiss.pairingAlgorithm}/$idt\n")
        val newEntry = TournamentEntry(idt, ChessPlatform.lichess, TournamentType.arena)
        todaysTournaments = todaysTournaments :+ newEntry
    end for
    
    if text.nonEmpty then
      val textString = text.foldLeft("")(_ + _)
      basicRequest
       .auth.bearer(TournamentEntry.token)
       .body(Map("message" -> textString))
       .post(uri"https://lichess.org/team/deutscher-schachbund-ev-offen/pm-all")
       .response(asString.getRight)
       .send(DefaultSyncBackend())

      val bsResponse = Bluesky.createSession(bsUser, bsPassword)
      val bsAccessToken = bsResponse._1
      val bsHandle = bsResponse._2
      val bsSuccess = Bluesky.createRecord(bsAccessToken, bsHandle, textString)
      print(textString)

