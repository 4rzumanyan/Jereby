package com.example.jereby.data.model

import androidx.room.Entity

@Entity(primaryKeys = ["tournamentId", "clubId"])
data class ClubStats(
    val tournamentId: Long,
    val clubId: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0
)