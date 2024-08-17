package de.qno.tournamentadmin

import upickle.default.*

case class AdminApi(base: String,
                    pairingAlgorithm: String,
                    createString: String,
                    time: String,
                    increment: String,
                    duration: String,
                    startdate: String
                   ) derives ReadWriter

object AdminApi:
  val pairingAlgo: String = lichessArena.pairingAlgorithm
  val createString = ""

  object lichessArena extends AdminApi (
    base = "https://lichess.org/api",
    pairingAlgorithm = "/tournament",
    createString = "",
    time = "clockTime",
    increment = "clockIncrement",
    duration = "minutes",
    startdate = "startDate"
  )

  object lichessSwiss extends AdminApi (
    base = "https://lichess.org/api",
    pairingAlgorithm = "/swiss",
    createString = s"/new/${TournamentAdmin.teamID}",
    time = "clock.limit",
    increment = "clock.increment",
    duration = "nbRounds",
    startdate = "startsAt"
  )
    
