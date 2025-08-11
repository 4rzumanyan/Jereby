package com.example.jereby.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("roundId"), Index("nextMatchId")])
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val position: Int,             // order within round
    val homeClubId: String,
    val awayClubId: String,
    val homePlayerId: Long,
    val awayPlayerId: Long,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val winnerClubId: String? = null,
    val nextMatchId: Long? = null
)
