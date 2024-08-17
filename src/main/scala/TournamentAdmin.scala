package de.qno.tournamentadmin

import upickle.default.*

import scala.collection.mutable.ListBuffer

case class TournamentAdmin()

object TournamentAdmin:
  val pathToResources: os.Path = os.pwd / "src" / "main" / "resources"
  val teamID = "deutscher-schachbund-ev-offen"
  val token: String = os.read.lines(pathToResources / "token.txt").head
  var nextTournaments: ListBuffer[String] = init()

  def init(): ListBuffer[String] =
    read[ListBuffer[String]](os.read(TournamentAdmin.pathToResources / "calendar.json"))

  def save(): Unit =
    os.write.over(TournamentAdmin.pathToResources / "calendar.json", write(TournamentAdmin.nextTournaments))
    
  def add(xs: List[String]): Unit =
    nextTournaments.addAll(xs) 

  enum ChessPlatform derives ReadWriter:
    case chesscom, lichess

  enum TournamentType derives ReadWriter:
    case swiss, arena
