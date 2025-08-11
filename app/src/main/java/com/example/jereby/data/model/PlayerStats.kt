package com.example.jereby.data.model

import androidx.room.Entity

@Entity(primaryKeys = ["tournamentId", "playerId"])
data class PlayerStats(
    val tournamentId: Long,
    val playerId: Long,
    val wins: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0
)
