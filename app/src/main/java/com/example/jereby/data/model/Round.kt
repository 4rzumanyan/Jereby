package com.example.jereby.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("tournamentId","indexInTournament", unique = true)])
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val indexInTournament: Int, // 0=first round
    val displayName: String
)
